package com.rescuesync.service;

import com.rescuesync.dto.RegionRequestDTO;
import com.rescuesync.dto.RegionResponseDTO;
import com.rescuesync.entity.AffectedRegion;
import com.rescuesync.exception.ConflictException;
import com.rescuesync.exception.DuplicateResourceException;
import com.rescuesync.exception.InvalidRegionDataException;
import com.rescuesync.exception.ResourceNotFoundException;
import com.rescuesync.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RegionService {

    private final AffectedRegionRepository regionRepository;
    private final AllocationRepository allocationRepository;
    private final ReliefRequestRepository reliefRequestRepository;
    private final RedistributionRecordRepository redistributionRecordRepository;
    private final RoadRepository roadRepository;
    private final ResourceRepository resourceRepository;
    private final ShelterRepository shelterRepository;
    private final RescueTeamRepository rescueTeamRepository;

    public RegionService(
            AffectedRegionRepository regionRepository,
            AllocationRepository allocationRepository,
            ReliefRequestRepository reliefRequestRepository,
            RedistributionRecordRepository redistributionRecordRepository,
            RoadRepository roadRepository,
            ResourceRepository resourceRepository,
            ShelterRepository shelterRepository,
            RescueTeamRepository rescueTeamRepository) {

        this.regionRepository = regionRepository;
        this.allocationRepository = allocationRepository;
        this.reliefRequestRepository = reliefRequestRepository;
        this.redistributionRecordRepository = redistributionRecordRepository;
        this.roadRepository = roadRepository;
        this.resourceRepository = resourceRepository;
        this.shelterRepository = shelterRepository;
        this.rescueTeamRepository = rescueTeamRepository;
    }

    // Get all disaster regions
    @Transactional(readOnly = true)
    public List<RegionResponseDTO> getAllRegions() {

        return regionRepository.findAllByOrderByPriorityScoreDesc()
                .stream()
                .map(RegionResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // Get a region using its ID
    @Transactional(readOnly = true)
    public RegionResponseDTO getRegionById(Integer id) {

        int regionId = Objects.requireNonNull(
                id, "Region ID must not be null.");

        AffectedRegion region = regionRepository.findById(regionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Disaster region with ID " + regionId + " not found."));

        return RegionResponseDTO.fromEntity(region);
    }

    // Create a new disaster region
    @Transactional
    public RegionResponseDTO createRegion(RegionRequestDTO request) {

        String trimmedName = request.getRegionName().trim();

        if (regionRepository.existsByRegionNameIgnoreCase(trimmedName)) {
            throw new DuplicateResourceException(
                    "Region name '" + trimmedName
                            + "' is already registered. Please use a unique identifier.");
        }

        validateRegionalMetrics(request);

        double priorityScore = computePriorityScore(
                request.getPopulationAffected(),
                request.getDisasterSeverity(),
                request.getInjuredPeople(),
                request.getVulnerablePeople(),
                request.getRoadAccessibility()
        );

        AffectedRegion region = new AffectedRegion();

        region.setRegionName(trimmedName);
        region.setLocation(request.getLocation().trim());
        region.setPopulationAffected(request.getPopulationAffected());
        region.setDisasterSeverity(request.getDisasterSeverity());
        region.setInjuredPeople(request.getInjuredPeople());
        region.setVulnerablePeople(request.getVulnerablePeople());
        region.setRoadAccessibility(request.getRoadAccessibility());
        region.setPriorityScore(priorityScore);
        region.setLatitude(request.getLatitude());
        region.setLongitude(request.getLongitude());

        AffectedRegion savedRegion = regionRepository.save(region);

        return RegionResponseDTO.fromEntity(savedRegion);
    }

    // Update an existing disaster region
    @Transactional
    public RegionResponseDTO updateRegion(
            Integer id,
            RegionRequestDTO request) {

        int regionId = Objects.requireNonNull(
                id, "Region ID must not be null.");

        AffectedRegion region = regionRepository.findById(regionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Disaster region with ID " + regionId + " not found."));

        String trimmedName = request.getRegionName().trim();

        if (!region.getRegionName().equalsIgnoreCase(trimmedName)) {

            Optional<AffectedRegion> existing =
                    regionRepository.findByRegionNameIgnoreCase(trimmedName);

            if (existing.isPresent()
                    && !existing.get().getRegionId().equals(regionId)) {

                throw new DuplicateResourceException(
                        "Region name '" + trimmedName
                                + "' is already taken by another sector.");
            }
        }

        validateRegionalMetrics(request);

        double priorityScore = computePriorityScore(
                request.getPopulationAffected(),
                request.getDisasterSeverity(),
                request.getInjuredPeople(),
                request.getVulnerablePeople(),
                request.getRoadAccessibility()
        );

        region.setRegionName(trimmedName);
        region.setLocation(request.getLocation().trim());
        region.setPopulationAffected(request.getPopulationAffected());
        region.setDisasterSeverity(request.getDisasterSeverity());
        region.setInjuredPeople(request.getInjuredPeople());
        region.setVulnerablePeople(request.getVulnerablePeople());
        region.setRoadAccessibility(request.getRoadAccessibility());
        region.setPriorityScore(priorityScore);
        region.setLatitude(request.getLatitude());
        region.setLongitude(request.getLongitude());

        AffectedRegion updatedRegion = regionRepository.save(region);

        return RegionResponseDTO.fromEntity(updatedRegion);
    }

    // Delete a region only when it has no dependent records
    @Transactional
    public void deleteRegion(Integer id) {

        int regionId = Objects.requireNonNull(
                id, "Region ID must not be null.");

        AffectedRegion region = regionRepository.findById(regionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Disaster region with ID " + regionId + " not found."));

        List<String> conflicts = new ArrayList<>();

        if (allocationRepository.existsByRegionRegionId(regionId)
                || allocationRepository.existsBySupplyRegionRegionId(regionId)) {

            conflicts.add("Active resource allocations");
        }

        if (reliefRequestRepository.existsByRegionRegionId(regionId)) {
            conflicts.add("Pending or fulfilled relief requests");
        }

        if (redistributionRecordRepository
                .existsBySourceRegionRegionIdOrDestRegionRegionId(
                        regionId, regionId)) {

            conflicts.add("Inter-regional redistribution logs");
        }

        if (roadRepository
                .existsBySourceRegionRegionIdOrDestinationRegionRegionId(
                        regionId, regionId)) {

            conflicts.add("Connected road network routes");
        }

        if (resourceRepository.existsByRegionRegionId(regionId)) {
            conflicts.add("Stored warehouse inventory");
        }

        if (shelterRepository.existsByRegionRegionId(regionId)) {
            conflicts.add("Evacuation shelter records");
        }

        if (rescueTeamRepository.existsByAssignedRegionRegionId(regionId)) {
            conflicts.add("Assigned emergency rescue teams");
        }

        if (!conflicts.isEmpty()) {

            throw new ConflictException(
                    "Cannot delete region '" + region.getRegionName()
                            + "' (ID: " + regionId
                            + ") because active dependencies exist: "
                            + String.join(", ", conflicts)
                            + ". Deleting this region would violate foreign-key integrity "
                            + "and corrupt disaster audit history. "
                            + "Safe alternative: update regional parameters "
                            + "to indicate deactivated or cleared status."
            );
        }

        regionRepository.delete(
                Objects.requireNonNull(region, "Region must not be null.")
        );
    }

    // Validate disaster region information
    private void validateRegionalMetrics(RegionRequestDTO request) {

        if (request.getPopulationAffected() > 0
                && request.getInjuredPeople() > request.getPopulationAffected()) {

            throw new InvalidRegionDataException(
                    "Injured count (" + request.getInjuredPeople()
                            + ") cannot exceed the total population affected ("
                            + request.getPopulationAffected() + ")."
            );
        }
    }

    // Calculate disaster priority score
    public static double computePriorityScore(
            int populationAffected,
            int disasterSeverity,
            int injuredPeople,
            int vulnerablePeople,
            double roadAccessibility) {

        double severityComponent =
                (disasterSeverity / 10.0) * 35.0;

        double injuryRatio = populationAffected > 0
                ? Math.min(1.0, (double) injuredPeople / populationAffected)
                : 0.0;

        double injuryComponent = injuryRatio * 25.0;

        double vulnerableRatio = populationAffected > 0
                ? Math.min(1.0, (double) vulnerablePeople / populationAffected)
                : 0.0;

        double vulnerableComponent = vulnerableRatio * 20.0;

        double popFactor = Math.min(
                1.0,
                Math.log10(Math.max(10, populationAffected)) / 4.0
        );

        double popComponent = popFactor * 10.0;

        double accessibilityUrgencyComponent =
                (1.0 - roadAccessibility) * 10.0;

        double total = severityComponent
                + injuryComponent
                + vulnerableComponent
                + popComponent
                + accessibilityUrgencyComponent;

        return Math.round(total * 100.0) / 100.0;
    }
}