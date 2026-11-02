
package com.rescuesync.service;

import com.rescuesync.dto.*;
import com.rescuesync.entity.AffectedRegion;
import com.rescuesync.entity.Allocation;
import com.rescuesync.entity.ReliefRequest;
import com.rescuesync.entity.Resource;
import com.rescuesync.exception.ResourceNotFoundException;
import com.rescuesync.repository.AffectedRegionRepository;
import com.rescuesync.repository.AllocationRepository;
import com.rescuesync.repository.ReliefRequestRepository;
import com.rescuesync.repository.ResourceRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AllocationService {

    private final AllocationRepository allocationRepository;
    private final ReliefRequestRepository reliefRequestRepository;
    private final ResourceRepository resourceRepository;
    private final AffectedRegionRepository affectedRegionRepository;
    private final RouteService routeService;

    // Null-safe helper methods
    private static int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private static double safeDouble(Double value) {
        return value != null ? value : 0.0;
    }

    // Higher priority first, then FIFO order
    private static final Comparator<ReliefRequest> PRIORITY_COMPARATOR = (a, b) -> {

        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;

        int cmp = Double.compare(
                safeDouble(b.getPriorityScore()),
                safeDouble(a.getPriorityScore())
        );

        if (cmp != 0) {
            return cmp;
        }

        return Integer.compare(
                safeInt(a.getRequestArrivalOrder()),
                safeInt(b.getRequestArrivalOrder())
        );
    };

    public AllocationService(
            AllocationRepository allocationRepository,
            ReliefRequestRepository reliefRequestRepository,
            ResourceRepository resourceRepository,
            AffectedRegionRepository affectedRegionRepository,
            RouteService routeService) {

        this.allocationRepository = allocationRepository;
        this.reliefRequestRepository = reliefRequestRepository;
        this.resourceRepository = resourceRepository;
        this.affectedRegionRepository = affectedRegionRepository;
        this.routeService = routeService;
    }

    // Retrieve all allocation records
    @Transactional(readOnly = true)
    public List<AllocationResponseDTO> getAllAllocations() {

        return allocationRepository.findAllByOrderByAllocationIdDesc()
                .stream()
                .map(AllocationResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // Retrieve allocations by request ID
    @Transactional(readOnly = true)
    public List<AllocationResponseDTO> getAllocationsByRequestId(Integer requestId) {

        if (requestId == null || !reliefRequestRepository.existsById(requestId)) {
            throw new ResourceNotFoundException(
                    "Relief request with ID " + requestId + " not found."
            );
        }

        return allocationRepository
                .findByRequestRequestIdOrderByAllocationIdDesc(requestId)
                .stream()
                .map(AllocationResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // Retrieve allocations by region ID
    @Transactional(readOnly = true)
    public List<AllocationResponseDTO> getAllocationsByRegionId(Integer regionId) {

        if (regionId == null || !affectedRegionRepository.existsById(regionId)) {
            throw new ResourceNotFoundException(
                    "Disaster region with ID " + regionId + " not found."
            );
        }

        return allocationRepository
                .findByRegionRegionIdOrderByAllocationIdDesc(regionId)
                .stream()
                .map(AllocationResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // Calculate allocation summary
    @Transactional(readOnly = true)
    public AllocationSummaryDTO getAllocationSummary() {

        long totalAllocations = allocationRepository.count();

        Long totalUnits = allocationRepository.sumTotalUnitsAllocated();
        long totalUnitsAllocated = totalUnits != null ? totalUnits : 0L;

        List<ReliefRequest> allRequests = reliefRequestRepository.findAll();

        long pendingCount = 0;
        long partialCount = 0;
        long fullyAllocatedCount = 0;

        for (ReliefRequest request : allRequests) {

            String status = request.getRequestStatus() != null
                    ? request.getRequestStatus().toUpperCase(Locale.ROOT)
                    : "PENDING";

            // String comparisons avoid dependency on service constants
            if ("ALLOCATED".equals(status)) {

                fullyAllocatedCount++;

            } else if ("PARTIALLY_ALLOCATED".equals(status)) {

                partialCount++;

            } else {

                pendingCount++;
            }
        }

        long activeQueueCount = pendingCount + partialCount;

        Map<String, Long> resourceDistribution = new HashMap<>();

        List<Allocation> allAllocations = allocationRepository.findAll();

        for (Allocation allocation : allAllocations) {

            String resourceName = allocation.getResourceName();

            if (resourceName != null) {

                int quantity = safeInt(allocation.getQuantityAllocated());

                resourceDistribution.merge(
                        resourceName,
                        (long) quantity,
                        Long::sum
                );
            }
        }

        return new AllocationSummaryDTO(
                totalAllocations,
                totalUnitsAllocated,
                pendingCount,
                partialCount,
                fullyAllocatedCount,
                activeQueueCount,
                resourceDistribution
        );
    }

    /*
     * Allocate resources to an individual relief request.
     * Surplus = max(0, available - reserved).
     * Reserved emergency resources are protected.
     */
    @Transactional
    public AllocationResultDTO allocateRequest(Integer requestId, String allocatedBy) {

        if (requestId == null) {
            throw new ResourceNotFoundException(
                    "Relief request ID cannot be null."
            );
        }

        ReliefRequest request = reliefRequestRepository.findById(requestId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Relief request #" + requestId + " not found."
                        )
                );

        int reqQty = safeInt(request.getRequiredQuantity());
        int allocQty = safeInt(request.getAllocatedQuantity());

        int remainingNeeded = Math.max(0, reqQty - allocQty);

        if (remainingNeeded <= 0) {

            if (!"ALLOCATED".equalsIgnoreCase(request.getRequestStatus())) {

                request.setRequestStatus("ALLOCATED");
                reliefRequestRepository.save(request);
            }

            return new AllocationResultDTO(
                    false,
                    "Request #" + requestId
                            + " is already fully satisfied ("
                            + allocQty + "/" + reqQty + " units).",
                    requestId,
                    0,
                    0,
                    request.getRequestStatus(),
                    null,
                    null,
                    null
            );
        }

        AffectedRegion destRegion = request.getRegion();

        if (destRegion == null || destRegion.getRegionId() == null) {

            return AllocationResultDTO.failure(
                    requestId,
                    "Destination disaster sector is missing or invalid."
            );
        }

        Integer destRegionId = destRegion.getRegionId();
        String resourceName = request.getResourceRequired();

        if (resourceName == null || resourceName.isBlank()) {

            return AllocationResultDTO.failure(
                    requestId,
                    "Required resource name is missing."
            );
        }

        Resource supplyStock = null;
        AffectedRegion supplyRegion = null;
        int supplySurplus = 0;
        RouteResponse selectedRoute = null;

        // STEP 1: Check local stock first (Local allocation remains first priority)
        Optional<Resource> localStockOpt =
                resourceRepository.findByRegionRegionIdAndResourceNameIgnoreCase(
                        destRegionId,
                        resourceName
                );

        if (localStockOpt.isPresent()) {

            Resource localResource = localStockOpt.get();

            int available = safeInt(localResource.getAvailableQuantity());
            int reserved = safeInt(localResource.getReservedQuantity());

            int surplus = Math.max(0, available - reserved);

            if (surplus > 0) {

                supplyStock = localResource;
                supplyRegion = destRegion;
                supplySurplus = surplus;
            }
        }

        // STEP 2: Find donor regions if local stock is unavailable
        // Route-aware donor selection: exclude unreachable donors and blocked roads
        if (supplyStock == null) {

            List<Resource> candidates =
                    resourceRepository.findByResourceNameIgnoreCaseAndRegionRegionIdNot(
                            resourceName,
                            destRegionId
                    );

            record DonorRouteMatch(Resource resource, int surplus, RouteResponse route) {}

            List<DonorRouteMatch> reachableDonors = new ArrayList<>();

            for (Resource candidate : candidates) {
                if (candidate == null || candidate.getRegion() == null || candidate.getRegion().getRegionId() == null) {
                    continue;
                }

                int available = safeInt(candidate.getAvailableQuantity());
                int reserved = safeInt(candidate.getReservedQuantity());
                int surplus = available - reserved;

                // Surplus rule: must hold surplus beyond emergency reserves
                if (surplus <= 0) {
                    continue;
                }

                Integer donorRegionId = candidate.getRegion().getRegionId();

                try {
                    // Check if an open transit corridor exists using Dijkstra
                    RouteResponse route = routeService.findShortestRoute(donorRegionId, destRegionId);

                    // Donor regions must only be selected if an OPEN route exists
                    if (route != null && route.isRouteAvailable() && route.getRegionPath() != null && !route.getRegionPath().isEmpty()) {
                        reachableDonors.add(new DonorRouteMatch(candidate, surplus, route));
                    }
                } catch (Exception e) {
                    // Ignore unreachable / routing exception and continue checking other candidates
                }
            }

            // Rank reachable donors: fastest travel time first, then shortest distance, then highest surplus
            reachableDonors.sort((d1, d2) -> {
                int cmpTime = Double.compare(d1.route.getTotalTravelTime(), d2.route.getTotalTravelTime());
                if (cmpTime != 0) {
                    return cmpTime;
                }

                int cmpDist = Double.compare(d1.route.getTotalDistanceKm(), d2.route.getTotalDistanceKm());
                if (cmpDist != 0) {
                    return cmpDist;
                }

                int cmpSurplus = Integer.compare(d2.surplus, d1.surplus);
                if (cmpSurplus != 0) {
                    return cmpSurplus;
                }

                return Integer.compare(
                        d1.resource.getRegion().getRegionId(),
                        d2.resource.getRegion().getRegionId()
                );
            });

            if (!reachableDonors.isEmpty()) {
                DonorRouteMatch bestMatch = reachableDonors.get(0);
                supplyStock = bestMatch.resource;
                supplyRegion = bestMatch.resource.getRegion();
                supplySurplus = bestMatch.surplus;
                selectedRoute = bestMatch.route;
            }
        }

        // STEP 3: Check available surplus
        if (supplyStock == null || supplyRegion == null || supplySurplus <= 0) {

            return AllocationResultDTO.failure(
                    requestId,
                    "No reachable donor region with surplus stock for '" + resourceName
                            + "' exists beyond local emergency reserves via open transit corridors. Allocation remains pending."
            );
        }

        // STEP 4: Calculate allocation quantity
        int toAllocate = Math.min(supplySurplus, remainingNeeded);

        if (toAllocate <= 0) {

            return AllocationResultDTO.failure(
                    requestId,
                    "Feasible allocation quantity is 0 units."
            );
        }

        // STEP 5: Deduct from inventory safely (Never allows negative inventory)
        int currentAvailable = safeInt(supplyStock.getAvailableQuantity());

        supplyStock.setAvailableQuantity(currentAvailable - toAllocate);
        resourceRepository.save(supplyStock);

        // STEP 6: Update relief request
        int newTotalAllocated = allocQty + toAllocate;

        request.setAllocatedQuantity(newTotalAllocated);

        String newStatus = (newTotalAllocated >= reqQty)
                ? "ALLOCATED"
                : "PARTIALLY_ALLOCATED";

        request.setRequestStatus(newStatus);
        reliefRequestRepository.save(request);

        // STEP 7: Save allocation history with actual Dijkstra route distance
        Allocation allocation = new Allocation();

        allocation.setRequest(request);
        allocation.setRegion(destRegion);
        allocation.setResourceName(resourceName);
        allocation.setQuantityAllocated(toAllocate);
        allocation.setSupplyRegion(supplyRegion);

        // Route distance: 0.0 km for local depot dispatch, Dijkstra distance for donor transit
        double routeDistance = (selectedRoute != null) ? selectedRoute.getTotalDistanceKm() : 0.0;
        allocation.setRouteDistanceKm(routeDistance);

        allocation.setAllocatedBy(
                allocatedBy != null ? allocatedBy : "SYSTEM"
        );

        Allocation savedAllocation = allocationRepository.save(allocation);

        // STEP 8: Prepare result
        int remainingDeficit = reqQty - newTotalAllocated;

        String successMsg;
        if (selectedRoute != null && !selectedRoute.getSourceRegionId().equals(destRegionId)) {
            successMsg = String.format(
                    "Successfully allocated %d units of %s to %s from %s depot via path %s (%.1f km, %.1f mins).",
                    toAllocate,
                    resourceName,
                    destRegion.getRegionName(),
                    supplyRegion.getRegionName(),
                    selectedRoute.getRegionPath(),
                    selectedRoute.getTotalDistanceKm(),
                    selectedRoute.getTotalTravelTime()
            );
        } else {
            successMsg = String.format(
                    "Successfully allocated %d units of %s to %s from local %s depot.",
                    toAllocate,
                    resourceName,
                    destRegion.getRegionName(),
                    supplyRegion.getRegionName()
            );
        }

        return new AllocationResultDTO(
                true,
                successMsg,
                requestId,
                toAllocate,
                remainingDeficit,
                newStatus,
                supplyRegion.getRegionId(),
                supplyRegion.getRegionName(),
                savedAllocation.getAllocationId()
        );
    }

    /*
     * Execute batch allocation using PriorityQueue.
     */
    @Transactional
    public BatchAllocationResultDTO executeBatchAllocation(String allocatedBy) {

        List<ReliefRequest> activeList =
                reliefRequestRepository
                        .findByRequestStatusInOrderByPriorityScoreDescRequestArrivalOrderAsc(
                                List.of(
                                        "PENDING",
                                        "PARTIALLY_ALLOCATED"
                                )
                        );

        PriorityQueue<ReliefRequest> priorityQueue =
                new PriorityQueue<>(PRIORITY_COMPARATOR);

        priorityQueue.addAll(activeList);

        List<AllocationResultDTO> results = new ArrayList<>();

        int successful = 0;
        int unfulfilled = 0;
        int totalUnits = 0;

        while (!priorityQueue.isEmpty()) {

            ReliefRequest current = priorityQueue.poll();

            AllocationResultDTO result =
                    allocateRequest(
                            current.getRequestId(),
                            allocatedBy
                    );

            results.add(result);

            if (result.isSuccess()) {

                successful++;

                totalUnits += safeInt(result.getAllocatedQuantity());

            } else {

                unfulfilled++;
            }
        }

        return new BatchAllocationResultDTO(
                results.size(),
                successful,
                unfulfilled,
                totalUnits,
                results
        );
    }
}

