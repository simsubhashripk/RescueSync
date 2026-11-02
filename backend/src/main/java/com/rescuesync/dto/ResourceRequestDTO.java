package com.rescuesync.dto;

import jakarta.validation.constraints.*;

public class ResourceRequestDTO {

    @NotNull(message = "Disaster region ID is required.")
    private Integer regionId;

    @NotBlank(message = "Resource name must not be empty.")
    @Size(max = 50, message = "Resource name must not exceed 50 characters.")
    private String resourceName;

    @NotNull(message = "Available quantity is required.")
    @Min(value = 0, message = "Available quantity must be non-negative.")
    private Integer availableQuantity;

    @NotNull(message = "Reserved quantity is required.")
    @Min(value = 0, message = "Reserved quantity must be non-negative.")
    private Integer reservedQuantity;

    public ResourceRequestDTO() {
    }

    public ResourceRequestDTO(Integer regionId, String resourceName, Integer availableQuantity, Integer reservedQuantity) {
        this.regionId = regionId;
        this.resourceName = resourceName;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = reservedQuantity;
    }

    public Integer getRegionId() {
        return regionId;
    }

    public void setRegionId(Integer regionId) {
        this.regionId = regionId;
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
}

