package com.rescuesync.service;

import com.rescuesync.dto.*;
import com.rescuesync.entity.AffectedRegion;
import com.rescuesync.entity.RedistributionRecord;
import com.rescuesync.entity.ReliefRequest;
import com.rescuesync.entity.Resource;
import com.rescuesync.exception.ConflictException;
import com.rescuesync.exception.ResourceNotFoundException;
import com.rescuesync.repository.AffectedRegionRepository;
import com.rescuesync.repository.RedistributionRecordRepository;
import com.rescuesync.repository.ReliefRequestRepository;
import com.rescuesync.repository.ResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RedistributionService {

    public static final String STATUS_RECOMMENDED = "RECOMMENDED";
    public static final String STATUS_COMPLETED = "COMPLETED";

    private final RedistributionRecordRepository redistributionRecordRepository;
    private final ResourceRepository resourceRepository;
    private final ReliefRequestRepository reliefRequestRepository;
    private final AffectedRegionRepository affectedRegionRepository;
    private final RouteService routeService;

    public RedistributionService(RedistributionRecordRepository redistributionRecordRepository,
                                 ResourceRepository resourceRepository,
                                 ReliefRequestRepository reliefRequestRepository,
                                 AffectedRegionRepository affectedRegionRepository,
                                 RouteService routeService) {
        this.redistributionRecordRepository = redistributionRecordRepository;
        this.resourceRepository = resourceRepository;
        this.reliefRequestRepository = reliefRequestRepository;
        this.affectedRegionRepository = affectedRegionRepository;
        this.routeService = routeService;
    }

    private static int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private static double safeDouble(Double value) {
        return value != null ? value : 0.0;
    }

    /**
     * Helper to compute aggregate unfulfilled demands by resource type for a given region.
     */
    private Map<String, Integer> getPendingDeficitsForRegion(Integer regionId) {
        Map<String, Integer> map = new HashMap<>();
        List<ReliefRequest> requests = reliefRequestRepository.findByRegionRegionId(regionId);
        for (ReliefRequest req : requests) {
            String status = req.getRequestStatus() != null ? req.getRequestStatus().toUpperCase(Locale.ROOT) : "PENDING";
            if (!"ALLOCATED".equals(status)) {
                String resName = req.getResourceRequired() != null ? req.getResourceRequired().trim().toLowerCase(Locale.ROOT) : "";
                if (!resName.isEmpty()) {
                    int reqQty = safeInt(req.getRequiredQuantity());
                    int allocQty = safeInt(req.getAllocatedQuantity());
                    int deficit = Math.max(0, reqQty - allocQty);
                    map.merge(resName, deficit, Integer::sum);
                }
            }
        }
        return map;
    }

    /**
     * 1. SHORTAGE DETECTION
     * UsableStock = max(0, availableQuantity - reservedQuantity)
     * Shortage = max(0, PendingDeficit - UsableStock)
     * Never count reserved stock as usable inventory.
     */
    @Transactional(readOnly = true)
    public List<ResourceAuditReportDTO> detectShortages() {
        List<ResourceAuditReportDTO> reports = new ArrayList<>();
        List<AffectedRegion> regions = affectedRegionRepository.findAll();

        for (AffectedRegion region : regions) {
            Integer regionId = region.getRegionId();
            Map<String, Integer> pendingDeficits = getPendingDeficitsForRegion(regionId);
            List<Resource> resources = resourceRepository.findByRegionRegionId(regionId);

            Map<String, Resource> resourceMap = new HashMap<>();
            for (Resource r : resources) {
                if (r.getResourceName() != null) {
                    resourceMap.put(r.getResourceName().trim().toLowerCase(Locale.ROOT), r);
                }
            }

            // Check every item with pending deficit
            for (Map.Entry<String, Integer> entry : pendingDeficits.entrySet()) {
                String resKey = entry.getKey();
                int pendingDeficit = entry.getValue();

                Resource res = resourceMap.get(resKey);
                int available = res != null ? safeInt(res.getAvailableQuantity()) : 0;
                int reserved = res != null ? safeInt(res.getReservedQuantity()) : 0;

                int usableStock = Math.max(0, available - reserved);
                int shortage = Math.max(0, pendingDeficit - usableStock);

                if (shortage > 0) {
                    String standardName = res != null ? res.getResourceName() : capitalize(resKey);
                    reports.add(new ResourceAuditReportDTO(
                            regionId,
                            region.getRegionName(),
                            standardName,
                            available,
                            reserved,
                            usableStock,
                            pendingDeficit,
                            shortage,
                            0,
                            safeDouble(region.getPriorityScore())
                    ));
                }
            }
        }

        // Sort by priority score descending (most critical sectors first)
        reports.sort((a, b) -> Double.compare(safeDouble(b.getPriorityScore()), safeDouble(a.getPriorityScore())));
        return reports;
    }

    /**
     * 2. SURPLUS DETECTION
     * Surplus = max(0, availableQuantity - reservedQuantity - PendingDeficit)
     * Protect all emergency reserves and donor-region pending requirements.
     */
    @Transactional(readOnly = true)
    public List<ResourceAuditReportDTO> detectSurpluses() {
        List<ResourceAuditReportDTO> reports = new ArrayList<>();
        List<AffectedRegion> regions = affectedRegionRepository.findAll();

        for (AffectedRegion region : regions) {
            Integer regionId = region.getRegionId();
            Map<String, Integer> pendingDeficits = getPendingDeficitsForRegion(regionId);
            List<Resource> resources = resourceRepository.findByRegionRegionId(regionId);

            for (Resource res : resources) {
                if (res.getResourceName() == null) continue;
                String resKey = res.getResourceName().trim().toLowerCase(Locale.ROOT);

                int available = safeInt(res.getAvailableQuantity());
                int reserved = safeInt(res.getReservedQuantity());
                int pendingDeficit = pendingDeficits.getOrDefault(resKey, 0);

                int surplus = Math.max(0, available - reserved - pendingDeficit);
                int usableStock = Math.max(0, available - reserved);

                if (surplus > 0) {
                    reports.add(new ResourceAuditReportDTO(
                            regionId,
                            region.getRegionName(),
                            res.getResourceName(),
                            available,
                            reserved,
                            usableStock,
                            pendingDeficit,
                            0,
                            surplus,
                            safeDouble(region.getPriorityScore())
                    ));
                }
            }
        }

        // Sort by surplus descending
        reports.sort((a, b) -> Integer.compare(safeInt(b.getSurplusQuantity()), safeInt(a.getSurplusQuantity())));
        return reports;
    }

    /**
     * 3. RECOMMENDATION WORKFLOW
     * Generates route-verified redistribution recommendations and persists them.
     * Sets initial status to RECOMMENDED.
     * Generating recommendations does NOT modify resource quantities.
     */
    @Transactional
    public List<RedistributionResponseDTO> generateRedistributionRecommendations() {
        List<ResourceAuditReportDTO> shortages = detectShortages();
        List<ResourceAuditReportDTO> surpluses = detectSurpluses();

        List<RedistributionRecord> generatedRecords = new ArrayList<>();

        if (shortages.isEmpty() || surpluses.isEmpty()) {
            return Collections.emptyList();
        }

        // Working pool to track candidate surplus during matching pass
        Map<String, Integer> workingSurplusPool = new HashMap<>();
        for (ResourceAuditReportDTO s : surpluses) {
            String poolKey = s.getRegionId() + ":" + s.getResourceName().trim().toLowerCase(Locale.ROOT);
            workingSurplusPool.put(poolKey, s.getSurplusQuantity());
        }

        // Process shortages in priority order
        for (ResourceAuditReportDTO shortage : shortages) {
            int neededAmount = shortage.getShortageQuantity();
            Integer destRegionId = shortage.getRegionId();
            String resourceName = shortage.getResourceName();
            String resKey = resourceName.trim().toLowerCase(Locale.ROOT);

            AffectedRegion destRegion = affectedRegionRepository.findById(destRegionId).orElse(null);
            if (destRegion == null) continue;

            // Find candidate donors with positive surplus for this resource
            record ReachableCandidate(ResourceAuditReportDTO audit, RouteResponse route, int availableSurplus) {}
            List<ReachableCandidate> viableCandidates = new ArrayList<>();

            for (ResourceAuditReportDTO candidate : surpluses) {
                if (neededAmount <= 0) break;
                if (candidate.getRegionId().equals(destRegionId)) continue; // Must be external
                if (!candidate.getResourceName().trim().equalsIgnoreCase(resourceName)) continue;

                String poolKey = candidate.getRegionId() + ":" + resKey;
                int currentSurplus = workingSurplusPool.getOrDefault(poolKey, 0);
                if (currentSurplus <= 0) continue;

                // Route verification with Dijkstra
                try {
                    RouteResponse route = routeService.findShortestRoute(candidate.getRegionId(), destRegionId);
                    if (route != null && route.isRouteAvailable() && route.getRegionPath() != null && !route.getRegionPath().isEmpty()) {
                        viableCandidates.add(new ReachableCandidate(candidate, route, currentSurplus));
                    }
                } catch (Exception ignored) {
                    // Exclude unreachable donor
                }
            }

            // Rank reachable donors: travel time ASC, distance ASC, surplus DESC
            viableCandidates.sort((c1, c2) -> {
                int cmpTime = Double.compare(c1.route.getTotalTravelTime(), c2.route.getTotalTravelTime());
                if (cmpTime != 0) return cmpTime;

                int cmpDist = Double.compare(c1.route.getTotalDistanceKm(), c2.route.getTotalDistanceKm());
                if (cmpDist != 0) return cmpDist;

                return Integer.compare(c2.availableSurplus, c1.availableSurplus);
            });

            // Match and create recommendations
            for (ReachableCandidate match : viableCandidates) {
                if (neededAmount <= 0) break;

                String poolKey = match.audit.getRegionId() + ":" + resKey;
                int currentSurplus = workingSurplusPool.getOrDefault(poolKey, 0);
                if (currentSurplus <= 0) continue;

                int transferQty = Math.min(neededAmount, currentSurplus);
                workingSurplusPool.put(poolKey, currentSurplus - transferQty);
                neededAmount -= transferQty;

                AffectedRegion srcRegion = affectedRegionRepository.findById(match.audit.getRegionId()).orElse(null);
                if (srcRegion == null) continue;

                RedistributionRecord record = new RedistributionRecord();
                record.setSourceRegion(srcRegion);
                record.setDestRegion(destRegion);
                record.setResourceName(resourceName);
                record.setQuantity(transferQty);
                record.setEstimatedTravelTime(match.route.getTotalTravelTime());
                record.setStatus(STATUS_RECOMMENDED);

                RedistributionRecord saved = redistributionRecordRepository.save(record);
                generatedRecords.add(saved);
            }
        }

        return generatedRecords.stream()
                .map(RedistributionResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 5. TRANSACTION SAFETY & EXECUTION
     * Approves and executes an individual recommended redistribution transfer.
     * Decrements donor stock and increments recipient stock inside a database transaction.
     * Protects reserved buffer: Never reduces donor stock below reserved_quantity.
     */
    @Transactional
    public RedistributionTransferResultDTO executeTransfer(Integer recordId) {
        if (recordId == null) {
            throw new ResourceNotFoundException("Redistribution record ID cannot be null.");
        }

        RedistributionRecord record = redistributionRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Redistribution record #" + recordId + " not found."));

        if (STATUS_COMPLETED.equalsIgnoreCase(record.getStatus())) {
            throw new ConflictException("Redistribution record #" + recordId + " is already COMPLETED.");
        }

        AffectedRegion sourceRegion = record.getSourceRegion();
        AffectedRegion destRegion = record.getDestRegion();
        String resName = record.getResourceName();
        int qty = safeInt(record.getQuantity());

        if (sourceRegion == null || destRegion == null) {
            return RedistributionTransferResultDTO.failure(recordId, "Invalid donor or recipient region reference.");
        }

        // Recheck donor stock immediately before execution
        Resource donorResource = resourceRepository.findByRegionRegionIdAndResourceNameIgnoreCase(sourceRegion.getRegionId(), resName)
                .orElseThrow(() -> new ConflictException("Source sector '" + sourceRegion.getRegionName() + "' has no registered stock for " + resName));

        int donorAvail = safeInt(donorResource.getAvailableQuantity());
        int donorResv = safeInt(donorResource.getReservedQuantity());
        int donorSurplus = Math.max(0, donorAvail - donorResv);

        if (donorSurplus < qty) {
            throw new ConflictException(String.format(
                    "Insufficient donor surplus stock for %s in %s. Available: %d, Reserved: %d (Surplus: %d), Required: %d.",
                    resName, sourceRegion.getRegionName(), donorAvail, donorResv, donorSurplus, qty
            ));
        }

        // 1. Deduct from donor safely
        donorResource.setAvailableQuantity(donorAvail - qty);
        resourceRepository.save(donorResource);

        // 2. Add to recipient (create resource record if recipient currently has none)
        Optional<Resource> recipientResourceOpt = resourceRepository.findByRegionRegionIdAndResourceNameIgnoreCase(destRegion.getRegionId(), resName);
        if (recipientResourceOpt.isPresent()) {
            Resource recResource = recipientResourceOpt.get();
            recResource.setAvailableQuantity(safeInt(recResource.getAvailableQuantity()) + qty);
            resourceRepository.save(recResource);
        } else {
            Resource newResource = new Resource();
            newResource.setRegion(destRegion);
            newResource.setResourceName(resName);
            newResource.setAvailableQuantity(qty);
            newResource.setReservedQuantity(0);
            resourceRepository.save(newResource);
        }

        // 3. Mark record as COMPLETED
        record.setStatus(STATUS_COMPLETED);
        redistributionRecordRepository.save(record);

        String successMsg = String.format("Successfully executed redistribution transfer of %d units of %s from %s to %s.",
                qty, resName, sourceRegion.getRegionName(), destRegion.getRegionName());

        return new RedistributionTransferResultDTO(
                true,
                successMsg,
                recordId,
                STATUS_COMPLETED,
                sourceRegion.getRegionName(),
                destRegion.getRegionName(),
                resName,
                qty
        );
    }

    /**
     * Executes all pending RECOMMENDED transfers in batch.
     */
    @Transactional
    public List<RedistributionTransferResultDTO> executeBatchRedistribution() {
        List<RedistributionRecord> pending = redistributionRecordRepository.findByStatusOrderByRecordIdDesc(STATUS_RECOMMENDED);
        List<RedistributionTransferResultDTO> results = new ArrayList<>();

        for (RedistributionRecord rec : pending) {
            try {
                RedistributionTransferResultDTO res = executeTransfer(rec.getRecordId());
                results.add(res);
            } catch (Exception e) {
                results.add(RedistributionTransferResultDTO.failure(rec.getRecordId(), e.getMessage()));
            }
        }
        return results;
    }

    /**
     * Retrieves all redistribution records ordered by record_id descending.
     */
    @Transactional(readOnly = true)
    public List<RedistributionResponseDTO> getAllRedistributionRecords() {
        return redistributionRecordRepository.findAllByOrderByRecordIdDesc().stream()
                .map(RedistributionResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all currently active recommendations (status = RECOMMENDED).
     */
    @Transactional(readOnly = true)
    public List<RedistributionResponseDTO> getActiveRecommendations() {
        return redistributionRecordRepository.findByStatusOrderByRecordIdDesc(STATUS_RECOMMENDED).stream()
                .map(RedistributionResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Aggregates summary statistics for the Redistribution Dashboard.
     */
    @Transactional(readOnly = true)
    public RedistributionSummaryDTO getRedistributionSummary() {
        List<ResourceAuditReportDTO> shortages = detectShortages();
        List<ResourceAuditReportDTO> surpluses = detectSurpluses();

        int totalShortages = shortages.size();
        int totalSurpluses = surpluses.size();

        int recommendedCount = (int) redistributionRecordRepository.countByStatus(STATUS_RECOMMENDED);
        int completedCount = (int) redistributionRecordRepository.countByStatus(STATUS_COMPLETED);

        Integer recUnits = redistributionRecordRepository.sumQuantityByStatus(STATUS_RECOMMENDED);
        Integer compUnits = redistributionRecordRepository.sumQuantityByStatus(STATUS_COMPLETED);

        int totalUnitsRecommended = recUnits != null ? recUnits : 0;
        int totalUnitsTransferred = compUnits != null ? compUnits : 0;

        return new RedistributionSummaryDTO(
                totalShortages,
                totalSurpluses,
                recommendedCount,
                completedCount,
                totalUnitsRecommended,
                totalUnitsTransferred
        );
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase(Locale.ROOT) + str.substring(1);
    }
}

