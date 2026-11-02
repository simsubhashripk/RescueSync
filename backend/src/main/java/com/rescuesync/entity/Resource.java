package com.rescuesync.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * JPA Entity for 'resources' table.
 */
@Entity
@Table(name = "resources", uniqueConstraints = {
    @UniqueConstraint(name = "uq_region_resource", columnNames = {"region_id", "resource_name"})
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resource_id")
    private Integer resourceId;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "region_id", nullable = false)
    private AffectedRegion region;

    @NotBlank
    @Size(max = 50)
    @Column(name = "resource_name", nullable = false, length = 50)
    private String resourceName;

    @NotNull
    @PositiveOrZero
    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity = 0;

    @NotNull
    @PositiveOrZero
    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity = 0;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public Resource() {
    }

    public Resource(AffectedRegion region, String resourceName, Integer availableQuantity, Integer reservedQuantity) {
        this.region = region;
        this.resourceName = resourceName;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = reservedQuantity;
    }

    // Getters and Setters
    public Integer getResourceId() {
        return resourceId;
    }

    public void setResourceId(Integer resourceId) {
        this.resourceId = resourceId;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

