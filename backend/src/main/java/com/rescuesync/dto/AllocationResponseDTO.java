package com.rescuesync.dto;

import com.rescuesync.entity.Allocation;
import java.time.LocalDateTime;

public class AllocationResponseDTO {
    private Integer allocationId;
    private Integer requestId;
    private Integer regionId;
    private String regionName;
    private String resourceName;
    private Integer quantityAllocated;
    private Integer supplyRegionId;
    private String supplyRegionName;
    private Double routeDistanceKm;
    private String allocatedBy;
    private LocalDateTime allocatedAt;

    public AllocationResponseDTO() {
    }

    public static AllocationResponseDTO fromEntity(Allocation entity) {
        if (entity == null) {
            return null;
        }

        AllocationResponseDTO dto = new AllocationResponseDTO();
        dto.setAllocationId(entity.getAllocationId());
        if (entity.getRequest() != null) {
            dto.setRequestId(entity.getRequest().getRequestId());
        }
        if (entity.getRegion() != null) {
            dto.setRegionId(entity.getRegion().getRegionId());
            dto.setRegionName(entity.getRegion().getRegionName());
        }
        dto.setResourceName(entity.getResourceName());
        dto.setQuantityAllocated(entity.getQuantityAllocated());
        if (entity.getSupplyRegion() != null) {
            dto.setSupplyRegionId(entity.getSupplyRegion().getRegionId());
            dto.setSupplyRegionName(entity.getSupplyRegion().getRegionName());
        } else {
            dto.setSupplyRegionName("Local Depot");
        }
        dto.setRouteDistanceKm(entity.getRouteDistanceKm());
        dto.setAllocatedBy(entity.getAllocatedBy());
        dto.setAllocatedAt(entity.getAllocatedAt());
        return dto;
    }

    public Integer getAllocationId() {
        return allocationId;
    }

    public void setAllocationId(Integer allocationId) {
        this.allocationId = allocationId;
    }

    public Integer getRequestId() {
        return requestId;
    }

    public void setRequestId(Integer requestId) {
        this.requestId = requestId;
    }

    public Integer getRegionId() {
        return regionId;
    }

    public void setRegionId(Integer regionId) {
        this.regionId = regionId;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public Integer getQuantityAllocated() {
        return quantityAllocated;
    }

    public void setQuantityAllocated(Integer quantityAllocated) {
        this.quantityAllocated = quantityAllocated;
    }

    public Integer getSupplyRegionId() {
        return supplyRegionId;
    }

    public void setSupplyRegionId(Integer supplyRegionId) {
        this.supplyRegionId = supplyRegionId;
    }

    public String getSupplyRegionName() {
        return supplyRegionName;
    }

    public void setSupplyRegionName(String supplyRegionName) {
        this.supplyRegionName = supplyRegionName;
    }

    public Double getRouteDistanceKm() {
        return routeDistanceKm;
    }

    public void setRouteDistanceKm(Double routeDistanceKm) {
        this.routeDistanceKm = routeDistanceKm;
    }

    public String getAllocatedBy() {
        return allocatedBy;
    }

    public void setAllocatedBy(String allocatedBy) {
        this.allocatedBy = allocatedBy;
    }

    public LocalDateTime getAllocatedAt() {
        return allocatedAt;
    }

    public void setAllocatedAt(LocalDateTime allocatedAt) {
        this.allocatedAt = allocatedAt;
    }
}

