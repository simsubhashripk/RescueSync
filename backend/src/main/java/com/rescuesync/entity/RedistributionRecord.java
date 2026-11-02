package com.rescuesync.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * JPA Entity for 'redistribution_records' table.
 */
@Entity
@Table(name = "redistribution_records")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RedistributionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Integer recordId;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "source_region_id", nullable = false)
    private AffectedRegion sourceRegion;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dest_region_id", nullable = false)
    private AffectedRegion destRegion;

    @NotBlank
    @Size(max = 50)
    @Column(name = "resource_name", nullable = false, length = 50)
    private String resourceName;

    @NotNull
    @Positive
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @NotNull
    @PositiveOrZero
    @Column(name = "estimated_travel_time", nullable = false)
    private Double estimatedTravelTime;

    @NotBlank
    @Size(max = 20)
    @Column(name = "status", nullable = false, length = 20)
    private String status = "RECOMMENDED";

    @Column(name = "recommended_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime recommendedAt;

    public RedistributionRecord() {
    }

    public RedistributionRecord(AffectedRegion sourceRegion, AffectedRegion destRegion,
                                String resourceName, Integer quantity, Double estimatedTravelTime, String status) {
        this.sourceRegion = sourceRegion;
        this.destRegion = destRegion;
        this.resourceName = resourceName;
        this.quantity = quantity;
        this.estimatedTravelTime = estimatedTravelTime;
        this.status = status;
    }

    // Getters and Setters
    public Integer getRecordId() {
        return recordId;
    }

    public void setRecordId(Integer recordId) {
        this.recordId = recordId;
    }

    public AffectedRegion getSourceRegion() {
        return sourceRegion;
    }

    public void setSourceRegion(AffectedRegion sourceRegion) {
        this.sourceRegion = sourceRegion;
    }

    public AffectedRegion getDestRegion() {
        return destRegion;
    }

    public void setDestRegion(AffectedRegion destRegion) {
        this.destRegion = destRegion;
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
}

