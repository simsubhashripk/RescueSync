package com.rescuesync.service;

import com.rescuesync.dto.ResourceRequestDTO;
import com.rescuesync.dto.ResourceResponseDTO;
import com.rescuesync.entity.AffectedRegion;
import com.rescuesync.entity.Resource;
import com.rescuesync.exception.ConflictException;
import com.rescuesync.exception.DuplicateResourceException;
import com.rescuesync.exception.ResourceNotFoundException;
import com.rescuesync.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final AffectedRegionRepository affectedRegionRepository;
    private final AllocationRepository allocationRepository;
    private final ReliefRequestRepository reliefRequestRepository;
    private final RedistributionRecordRepository redistributionRecordRepository;

    public ResourceService(ResourceRepository resourceRepository,
                           AffectedRegionRepository affectedRegionRepository,
                           AllocationRepository allocationRepository,
                           ReliefRequestRepository reliefRequestRepository,
                           RedistributionRecordRepository redistributionRecordRepository) {
        this.resourceRepository = resourceRepository;
        this.affectedRegionRepository = affectedRegionRepository;
        this.allocationRepository = allocationRepository;
        this.reliefRequestRepository = reliefRequestRepository;
        this.redistributionRecordRepository = redistributionRecordRepository;
    }

    /**
     * Retrieve all resource inventory records across all disaster sectors.
     */
    @Transactional(readOnly = true)
    public List<ResourceResponseDTO> getAllResources() {
        return resourceRepository.findAllByOrderByRegionRegionNameAscResourceNameAsc().stream()
                .map(ResourceResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve a specific resource by ID.
     */
    @Transactional(readOnly = true)
    public ResourceResponseDTO getResourceById(Integer id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource record with ID " + id + " not found."));
        return ResourceResponseDTO.fromEntity(resource);
    }

    /**
     * Retrieve all resources assigned to a particular disaster region.
     */
    @Transactional(readOnly = true)
    public List<ResourceResponseDTO> getResourcesByRegion(Integer regionId) {
        if (!affectedRegionRepository.existsById(regionId)) {
            throw new ResourceNotFoundException("Disaster region with ID " + regionId + " does not exist.");
        }
        return resourceRepository.findByRegionRegionIdOrderByResourceNameAsc(regionId).stream()
                .map(ResourceResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Add a new resource item to a disaster region.
     * Enforces unique resource_name per region and non-negative quantities.
     */
    @Transactional
    public ResourceResponseDTO createResource(ResourceRequestDTO request) {
        AffectedRegion region = affectedRegionRepository.findById(request.getRegionId())
                .orElseThrow(() -> new ResourceNotFoundException("Disaster region with ID " + request.getRegionId() + " not found."));

        String trimmedName = request.getResourceName().trim();
        if (resourceRepository.existsByRegionRegionIdAndResourceNameIgnoreCase(region.getRegionId(), trimmedName)) {
            throw new DuplicateResourceException("Resource '" + trimmedName + "' is already registered for region '"
                    + region.getRegionName() + "'. Please update the existing stock instead.");
        }

        Resource resource = new Resource();
        resource.setRegion(region);
        resource.setResourceName(trimmedName);
        resource.setAvailableQuantity(request.getAvailableQuantity());
        resource.setReservedQuantity(request.getReservedQuantity());

        Resource saved = resourceRepository.save(resource);
        return ResourceResponseDTO.fromEntity(saved);
    }

    /**
     * Update an existing resource record's quantities or details.
     */
    @Transactional
    public ResourceResponseDTO updateResource(Integer id, ResourceRequestDTO request) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource record with ID " + id + " not found."));

        AffectedRegion region = affectedRegionRepository.findById(request.getRegionId())
                .orElseThrow(() -> new ResourceNotFoundException("Disaster region with ID " + request.getRegionId() + " not found."));

        String trimmedName = request.getResourceName().trim();
        boolean regionChanged = !resource.getRegion().getRegionId().equals(region.getRegionId());
        boolean nameChanged = !resource.getResourceName().equalsIgnoreCase(trimmedName);

        if (regionChanged || nameChanged) {
            Optional<Resource> collision = resourceRepository.findByRegionRegionIdAndResourceNameIgnoreCase(region.getRegionId(), trimmedName);
            if (collision.isPresent() && !collision.get().getResourceId().equals(id)) {
                throw new DuplicateResourceException("Resource '" + trimmedName + "' already exists in region '"
                        + region.getRegionName() + "'.");
            }
        }

        resource.setRegion(region);
        resource.setResourceName(trimmedName);
        resource.setAvailableQuantity(request.getAvailableQuantity());
        resource.setReservedQuantity(request.getReservedQuantity());

        Resource updated = resourceRepository.save(resource);
        return ResourceResponseDTO.fromEntity(updated);
    }

    /**
     * Safely delete a resource inventory record.
     * Prevents deletion if referenced by allocations, relief requests, or redistribution logs.
     */
    @Transactional
    public void deleteResource(Integer id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource record with ID " + id + " not found."));

        Integer regionId = resource.getRegion().getRegionId();
        String resourceName = resource.getResourceName();

        List<String> dependencies = new ArrayList<>();
        if (allocationRepository.existsByRegionRegionIdAndResourceNameIgnoreCase(regionId, resourceName)
                || allocationRepository.existsBySupplyRegionRegionIdAndResourceNameIgnoreCase(regionId, resourceName)) {
            dependencies.add("Historical or active resource allocations");
        }
        if (reliefRequestRepository.existsByRegionRegionIdAndResourceRequiredIgnoreCase(regionId, resourceName)) {
            dependencies.add("Relief requests registered for this resource");
        }
        if (redistributionRecordRepository.existsBySourceRegionRegionIdAndResourceNameIgnoreCase(regionId, resourceName)
                || redistributionRecordRepository.existsByDestRegionRegionIdAndResourceNameIgnoreCase(regionId, resourceName)) {
            dependencies.add("Inter-regional redistribution logs");
        }

        if (!dependencies.isEmpty()) {
            throw new ConflictException("Cannot delete resource '" + resourceName + "' in sector '"
                    + resource.getRegion().getRegionName() + "' (ID: " + id
                    + ") because active dependencies exist: " + String.join(", ", dependencies)
                    + ". Deleting this inventory record would corrupt historical allocations and audit traceability. "
                    + "Safe alternative: set available quantity to 0 instead of deleting.");
        }

        resourceRepository.delete(resource);
    }
}

