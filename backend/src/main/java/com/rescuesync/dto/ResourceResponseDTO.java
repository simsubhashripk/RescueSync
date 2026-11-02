package com.rescuesync.dto;

import java.time.LocalDateTime;

import com.rescuesync.entity.Resource;

public class ResourceResponseDTO {

    private Integer resourceId;
    private Integer regionId;
    private String regionName;
    private String resourceName;
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private Integer surplusQuantity;
    private LocalDateTime updatedAt;

    public ResourceResponseDTO() {
    }

    public static ResourceResponseDTO fromEntity(Resource resource) {

        if (resource == null) {
            return null;
        }

        ResourceResponseDTO dto = new ResourceResponseDTO();

        // Resource identification
        dto.setResourceId(resource.getResourceId());

        // Region information
        if (resource.getRegion() != null) {
            dto.setRegionId(resource.getRegion().getRegionId());
            dto.setRegionName(resource.getRegion().getRegionName());
        }

        // Resource information
        dto.setResourceName(resource.getResourceName());

        // Get quantities safely
        Integer available = resource.getAvailableQuantity();
        Integer reserved = resource.getReservedQuantity();

        dto.setAvailableQuantity(available);
        dto.setReservedQuantity(reserved);

        // Convert nullable Integer values safely
        int avail = 0;
        int resv = 0;

        if (available != null) {
            avail = available;
        }

        if (reserved != null) {
            resv = reserved;
        }

        // Calculate redistributable surplus
        int surplus = Math.max(0, avail - resv);

        dto.setSurplusQuantity(surplus);

        // Last updated timestamp
        dto.setUpdatedAt(resource.getUpdatedAt());

        return dto;
    }

    // Getters and Setters

    public Integer getResourceId() {
        return resourceId;
    }

    public void setResourceId(Integer resourceId) {
        this.resourceId = resourceId;
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

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public Integer getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public Integer getSurplusQuantity() {
        return surplusQuantity;
    }

    public void setSurplusQuantity(Integer surplusQuantity) {
        this.surplusQuantity = surplusQuantity;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}