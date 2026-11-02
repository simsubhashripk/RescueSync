
package com.rescuesync.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rescuesync.dto.AdminDashboardSummaryDTO;
import com.rescuesync.dto.RegionAnalyticsDTO;
import com.rescuesync.dto.RescueTeamDTO;
import com.rescuesync.entity.AffectedRegion;
import com.rescuesync.entity.ReliefRequest;
import com.rescuesync.entity.RescueTeam;
import com.rescuesync.entity.Resource;
import com.rescuesync.repository.AffectedRegionRepository;
import com.rescuesync.repository.AllocationRepository;
import com.rescuesync.repository.RedistributionRecordRepository;
import com.rescuesync.repository.ReliefRequestRepository;
import com.rescuesync.repository.RescueTeamRepository;
import com.rescuesync.repository.ResourceRepository;

/**
 * Service for aggregating system-wide metrics, regional analytics,
 * and rescue team deployments from real database records.
 */
@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final AffectedRegionRepository affectedRegionRepository;
    private final ResourceRepository resourceRepository;
    private final ReliefRequestRepository reliefRequestRepository;
    private final AllocationRepository allocationRepository;
    private final RedistributionRecordRepository redistributionRecordRepository;
    private final RescueTeamRepository rescueTeamRepository;

    public AnalyticsService(
            AffectedRegionRepository affectedRegionRepository,
            ResourceRepository resourceRepository,
            ReliefRequestRepository reliefRequestRepository,
            AllocationRepository allocationRepository,
            RedistributionRecordRepository redistributionRecordRepository,
            RescueTeamRepository rescueTeamRepository) {

        this.affectedRegionRepository = affectedRegionRepository;
        this.resourceRepository = resourceRepository;
        this.reliefRequestRepository = reliefRequestRepository;
        this.allocationRepository = allocationRepository;
        this.redistributionRecordRepository = redistributionRecordRepository;
        this.rescueTeamRepository = rescueTeamRepository;
    }

    /**
     * Safely converts Integer to int.
     * Returns zero if the value is null.
     */
    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * Safely converts Integer to long.
     * Returns zero if the value is null.
     */
    private long safeLong(Integer value) {
        return value == null ? 0L : value.longValue();
    }

    /**
     * Builds real-time executive dashboard KPIs from database records.
     */
    public AdminDashboardSummaryDTO getAdminDashboardSummary() {

        AdminDashboardSummaryDTO summary = new AdminDashboardSummaryDTO();

        List<AffectedRegion> regions = affectedRegionRepository.findAll();
        List<Resource> resources = resourceRepository.findAll();
        List<ReliefRequest> requests = reliefRequestRepository.findAll();

        summary.setTotalAffectedRegions(regions.size());

        long totalAvailable = 0;
        long totalReserved = 0;

        for (Resource r : resources) {
            totalAvailable += safeLong(r.getAvailableQuantity());
            totalReserved += safeLong(r.getReservedQuantity());
        }

        summary.setTotalAvailableResources(totalAvailable);
        summary.setTotalReservedResources(totalReserved);

        long pendingRequests = 0;
        long allocatedRequests = 0;

        for (ReliefRequest req : requests) {

            String status = req.getRequestStatus();

            if ("PENDING".equalsIgnoreCase(status)
                    || "PARTIALLY_ALLOCATED".equalsIgnoreCase(status)) {

                pendingRequests++;

            } else if ("ALLOCATED".equalsIgnoreCase(status)) {

                allocatedRequests++;
            }
        }

        summary.setTotalPendingRequests(pendingRequests);
        summary.setTotalAllocatedRequests(allocatedRequests);

        summary.setTotalCompletedAllocations(allocationRepository.count());

        Long allocatedUnits = allocationRepository.sumTotalUnitsAllocated();

        summary.setTotalAllocatedUnits(
                allocatedUnits != null ? allocatedUnits : 0L
        );

        summary.setTotalRescueTeams(rescueTeamRepository.count());

        summary.setDeployedRescueTeams(
                rescueTeamRepository.countByAvailabilityIgnoreCase("DEPLOYED")
        );

        summary.setAvailableRescueTeams(
                rescueTeamRepository.countByAvailabilityIgnoreCase("AVAILABLE")
        );

        summary.setTotalRedistributionOperations(
                redistributionRecordRepository.count()
        );

        summary.setCompletedRedistributions(
                redistributionRecordRepository.countByStatus("COMPLETED")
        );

        summary.setRecommendedRedistributions(
                redistributionRecordRepository.countByStatus("RECOMMENDED")
        );

        Integer redistributedUnits =
                redistributionRecordRepository.sumQuantityByStatus("COMPLETED");

        summary.setTotalRedistributedUnits(
                safeLong(redistributedUnits)
        );

        long totalPop = 0;
        int maxSeverity = 0;
        String maxSevRegion = "None";

        for (AffectedRegion reg : regions) {

            if (reg.getPopulationAffected() != null) {
                totalPop += reg.getPopulationAffected();
            }

            if (reg.getDisasterSeverity() != null
                    && reg.getDisasterSeverity() > maxSeverity) {

                maxSeverity = reg.getDisasterSeverity();
                maxSevRegion = reg.getRegionName();
            }
        }

        summary.setTotalPopulationAffected(totalPop);
        summary.setHighestSeverityLevel(maxSeverity);
        summary.setHighestSeverityRegion(maxSevRegion);

        // Overall disaster status evaluation
        if (maxSeverity >= 8 && pendingRequests > 0) {

            summary.setOverallDisasterStatus("CRITICAL - ACTIVE RELIEF");

        } else if (maxSeverity >= 5) {

            summary.setOverallDisasterStatus("HIGH ALERT");

        } else {

            summary.setOverallDisasterStatus("STABLE");
        }

        return summary;
    }

    /**
     * Aggregates regional analytics:
     * disaster severity, population, stock, deficits and assigned teams.
     */
    public List<RegionAnalyticsDTO> getRegionAnalytics() {

        List<AffectedRegion> regions =
                affectedRegionRepository.findAllByOrderByPriorityScoreDesc();

        List<Resource> allResources = resourceRepository.findAll();

        List<ReliefRequest> allRequests = reliefRequestRepository.findAll();

        List<RescueTeam> allTeams = rescueTeamRepository.findAll();

        List<RegionAnalyticsDTO> dtos = new ArrayList<>();

        for (AffectedRegion reg : regions) {

            RegionAnalyticsDTO dto = new RegionAnalyticsDTO();

            dto.setRegionId(reg.getRegionId());
            dto.setRegionName(reg.getRegionName());
            dto.setLocation(reg.getLocation());
            dto.setDisasterSeverity(reg.getDisasterSeverity());
            dto.setPriorityScore(reg.getPriorityScore());
            dto.setInjuredPeople(reg.getInjuredPeople());
            dto.setVulnerablePeople(reg.getVulnerablePeople());
            dto.setPopulationAffected(reg.getPopulationAffected());
            dto.setRoadAccessibility(reg.getRoadAccessibility());

            // Regional resource calculations
            long avail = 0;
            long resv = 0;

            for (Resource r : allResources) {

                if (r.getRegion() != null
                        && reg.getRegionId().equals(r.getRegion().getRegionId())) {

                    avail += safeLong(r.getAvailableQuantity());
                    resv += safeLong(r.getReservedQuantity());
                }
            }

            dto.setTotalAvailableStock(avail);
            dto.setTotalReservedStock(resv);

            // Regional relief request calculations
            long totalRequired = 0;
            long totalAlloc = 0;

            for (ReliefRequest req : allRequests) {

                if (req.getRegion() != null
                        && reg.getRegionId().equals(req.getRegion().getRegionId())) {

                    int reqQty = safeInt(req.getRequiredQuantity());
                    int allocQty = safeInt(req.getAllocatedQuantity());

                    totalRequired += reqQty;
                    totalAlloc += allocQty;
                }
            }

            long pendingDeficit = Math.max(0, totalRequired - totalAlloc);

            dto.setPendingRequiredUnits(pendingDeficit);
            dto.setAllocatedUnits(totalAlloc);

            // Relief fulfillment percentage
            if (totalRequired > 0) {

                double pct = (totalAlloc * 100.0) / totalRequired;

                dto.setFulfillmentPercentage(
                        Math.round(pct * 100.0) / 100.0
                );

            } else {

                dto.setFulfillmentPercentage(100.0);
            }

            // Assigned rescue teams
            List<String> assignedTeamNames = allTeams.stream()
                    .filter(t ->
                            t.getAssignedRegion() != null
                                    && reg.getRegionId().equals(
                                            t.getAssignedRegion().getRegionId()
                                    )
                    )
                    .map(t ->
                            t.getTeamName() + " (" + t.getAvailability() + ")"
                    )
                    .collect(Collectors.toList());

            dto.setAssignedTeams(assignedTeamNames);

            dtos.add(dto);
        }

        return dtos;
    }

    /**
     * Retrieves all rescue teams with assigned region names.
     */
    public List<RescueTeamDTO> getAllRescueTeams() {

        return rescueTeamRepository.findAllByOrderByTeamIdAsc()
                .stream()
                .map(t -> new RescueTeamDTO(
                        t.getTeamId(),
                        t.getTeamName(),
                        t.getAvailability(),
                        t.getAssignedRegion() != null
                                ? t.getAssignedRegion().getRegionId()
                                : null,
                        t.getAssignedRegion() != null
                                ? t.getAssignedRegion().getRegionName()
                                : "Unassigned"
                ))
                .collect(Collectors.toList());
    }
}

