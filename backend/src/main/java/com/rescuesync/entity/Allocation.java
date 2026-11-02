package com.rescuesync.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * JPA Entity for 'allocations' table.
 */
@Entity
@Table(name = "allocations")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Allocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "allocation_id")
    private Integer allocationId;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "request_id", nullable = false)
    private ReliefRequest request;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "region_id", nullable = false)
    private AffectedRegion region;

    @NotBlank
    @Size(max = 50)
    @Column(name = "resource_name", nullable = false, length = 50)
    private String resourceName;

    @NotNull
    @Positive
    @Column(name = "quantity_allocated", nullable = false)
    private Integer quantityAllocated;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "supply_region_id", nullable = true)
    private AffectedRegion supplyRegion;

    @NotNull
    @PositiveOrZero
    @Column(name = "route_distance_km", nullable = false)
    private Double routeDistanceKm = 0.0;

    @NotBlank
    @Size(max = 50)
    @Column(name = "allocated_by", nullable = false, length = 50)
    private String allocatedBy = "SYSTEM";

    @Column(name = "allocated_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime allocatedAt;

    public Allocation() {
    }

    public Allocation(ReliefRequest request, AffectedRegion region, String resourceName,
                      Integer quantityAllocated, AffectedRegion supplyRegion,
                      Double routeDistanceKm, String allocatedBy) {
        this.request = request;
        this.region = region;
        this.resourceName = resourceName;
        this.quantityAllocated = quantityAllocated;
        this.supplyRegion = supplyRegion;
        this.routeDistanceKm = routeDistanceKm;
        this.allocatedBy = allocatedBy;
    }

    // Getters and Setters
    public Integer getAllocationId() {
        return allocationId;
    }

    public void setAllocationId(Integer allocationId) {
        this.allocationId = allocationId;
    }

    public ReliefRequest getRequest() {
        return request;
    }

    public void setRequest(ReliefRequest request) {
        this.request = request;
    }

    public AffectedRegion getRegion() {
        return region;
    }

    public void setRegion(AffectedRegion region) {
        this.region = region;
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

    public AffectedRegion getSupplyRegion() {
        return supplyRegion;
    }

    public void setSupplyRegion(AffectedRegion supplyRegion) {
        this.supplyRegion = supplyRegion;
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

