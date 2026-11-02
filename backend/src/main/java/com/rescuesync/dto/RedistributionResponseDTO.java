package com.rescuesync.dto;

import com.rescuesync.entity.RedistributionRecord;
import java.time.LocalDateTime;
import java.util.List;

public class RedistributionResponseDTO {
    private Integer recordId;
    private Integer sourceRegionId;
    private String sourceRegionName;
    private Integer destRegionId;
    private String destRegionName;
    private String resourceName;
    private Integer quantity;
    private Double estimatedTravelTime;
    private String status;
    private LocalDateTime recommendedAt;
    private List<Integer> transitPath;

    public RedistributionResponseDTO() {
    }

    public static RedistributionResponseDTO fromEntity(RedistributionRecord entity) {
        if (entity == null) {
            return null;
        }

        RedistributionResponseDTO dto = new RedistributionResponseDTO();
        dto.setRecordId(entity.getRecordId());
        if (entity.getSourceRegion() != null) {
            dto.setSourceRegionId(entity.getSourceRegion().getRegionId());
            dto.setSourceRegionName(entity.getSourceRegion().getRegionName());
        }
        if (entity.getDestRegion() != null) {
            dto.setDestRegionId(entity.getDestRegion().getRegionId());
            dto.setDestRegionName(entity.getDestRegion().getRegionName());
        }
        dto.setResourceName(entity.getResourceName());
        dto.setQuantity(entity.getQuantity());
        dto.setEstimatedTravelTime(entity.getEstimatedTravelTime());
        dto.setStatus(entity.getStatus());
        dto.setRecommendedAt(entity.getRecommendedAt());
        return dto;
    }

    public Integer getRecordId() {
        return recordId;
    }

    public void setRecordId(Integer recordId) {
        this.recordId = recordId;
    }

    public Integer getSourceRegionId() {
        return sourceRegionId;
    }

    public void setSourceRegionId(Integer sourceRegionId) {
        this.sourceRegionId = sourceRegionId;
    }

    public String getSourceRegionName() {
        return sourceRegionName;
    }

    public void setSourceRegionName(String sourceRegionName) {
        this.sourceRegionName = sourceRegionName;
    }

    public Integer getDestRegionId() {
        return destRegionId;
    }

    public void setDestRegionId(Integer destRegionId) {
        this.destRegionId = destRegionId;
    }

    public String getDestRegionName() {
        return destRegionName;
    }

    public void setDestRegionName(String destRegionName) {
        this.destRegionName = destRegionName;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getEstimatedTravelTime() {
        return estimatedTravelTime;
    }

    public void setEstimatedTravelTime(Double estimatedTravelTime) {
        this.estimatedTravelTime = estimatedTravelTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getRecommendedAt() {
        return recommendedAt;
    }

    public void setRecommendedAt(LocalDateTime recommendedAt) {
        this.recommendedAt = recommendedAt;
    }

    public List<Integer> getTransitPath() {
        return transitPath;
    }

    public void setTransitPath(List<Integer> transitPath) {
        this.transitPath = transitPath;
    }
}

