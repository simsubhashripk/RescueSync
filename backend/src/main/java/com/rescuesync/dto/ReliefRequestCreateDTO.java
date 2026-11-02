package com.rescuesync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class ReliefRequestCreateDTO {

    @NotNull(message = "Disaster region ID is required.")
    private Integer regionId;

    @NotBlank(message = "Resource required must not be empty.")
    @Size(max = 50, message = "Resource required name must not exceed 50 characters.")
    private String resourceRequired;

    @NotNull(message = "Required quantity is required.")
    @Positive(message = "Required quantity must be greater than zero.")
    private Integer requiredQuantity;

    public ReliefRequestCreateDTO() {
    }

    public ReliefRequestCreateDTO(Integer regionId, String resourceRequired, Integer requiredQuantity) {
        this.regionId = regionId;
        this.resourceRequired = resourceRequired;
        this.requiredQuantity = requiredQuantity;
    }

    public Integer getRegionId() {
        return regionId;
    }

    public void setRegionId(Integer regionId) {
        this.regionId = regionId;
    }

    public String getResourceRequired() {
        return resourceRequired;
    }

    public void setResourceRequired(String resourceRequired) {
        this.resourceRequired = resourceRequired;
    }

    public Integer getRequiredQuantity() {
        return requiredQuantity;
    }

    public void setRequiredQuantity(Integer requiredQuantity) {
        this.requiredQuantity = requiredQuantity;
    }
}

