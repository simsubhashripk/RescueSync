package com.rescuesync.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * JPA Entity for 'relief_requests' table.
 */
@Entity
@Table(name = "relief_requests")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ReliefRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Integer requestId;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "region_id", nullable = false)
    private AffectedRegion region;

    @NotBlank
    @Size(max = 50)
    @Column(name = "resource_required", nullable = false, length = 50)
    private String resourceRequired;

    @NotNull
    @Positive
    @Column(name = "required_quantity", nullable = false)
    private Integer requiredQuantity;

    @NotNull
    @PositiveOrZero
    @Column(name = "allocated_quantity", nullable = false)
    private Integer allocatedQuantity = 0;

    @NotNull
    @Column(name = "priority_score", nullable = false)
    private Double priorityScore;

    @NotNull
    @Column(name = "request_arrival_order", nullable = false)
    private Integer requestArrivalOrder;

    @NotBlank
    @Size(max = 25)
    @Column(name = "request_status", nullable = false, length = 25)
    private String requestStatus = "PENDING";

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    public ReliefRequest() {
    }

    public ReliefRequest(AffectedRegion region, String resourceRequired, Integer requiredQuantity,
                         Integer allocatedQuantity, Double priorityScore, Integer requestArrivalOrder,
                         String requestStatus) {
        this.region = region;
        this.resourceRequired = resourceRequired;
        this.requiredQuantity = requiredQuantity;
        this.allocatedQuantity = allocatedQuantity;
        this.priorityScore = priorityScore;
        this.requestArrivalOrder = requestArrivalOrder;
        this.requestStatus = requestStatus;
    }

    // Getters and Setters
    public Integer getRequestId() {
        return requestId;
    }

    public void setRequestId(Integer requestId) {
        this.requestId = requestId;
    }

    public AffectedRegion getRegion() {
        return region;
    }

    public void setRegion(AffectedRegion region) {
        this.region = region;
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

    public Integer getAllocatedQuantity() {
        return allocatedQuantity;
    }

    public void setAllocatedQuantity(Integer allocatedQuantity) {
        this.allocatedQuantity = allocatedQuantity;
    }

    public Double getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(Double priorityScore) {
        this.priorityScore = priorityScore;
    }

    public Integer getRequestArrivalOrder() {
        return requestArrivalOrder;
    }

    public void setRequestArrivalOrder(Integer requestArrivalOrder) {
        this.requestArrivalOrder = requestArrivalOrder;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

