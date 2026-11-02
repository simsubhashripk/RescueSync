package com.rescuesync.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

/**
 * JPA Entity for 'roads' table.
 */
@Entity
@Table(name = "roads", uniqueConstraints = {
    @UniqueConstraint(name = "uq_road_endpoints", columnNames = {"source_region_id", "destination_region_id"})
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Road {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "road_id")
    private Integer roadId;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "source_region_id", nullable = false)
    private AffectedRegion sourceRegion;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "destination_region_id", nullable = false)
    private AffectedRegion destinationRegion;

    @NotNull
    @Positive
    @Column(name = "travel_time", nullable = false)
    private Double travelTime;

    @NotNull
    @Positive
    @Column(name = "distance_km", nullable = false)
    private Double distanceKm = 15.0;

    @Size(max = 100)
    @Column(name = "road_name", length = 100)
    private String roadName;

    @NotBlank
    @Size(max = 20)
    @Column(name = "accessibility_status", nullable = false, length = 20)
    private String accessibilityStatus = "OPEN";

    public Road() {
    }

    public Road(AffectedRegion sourceRegion, AffectedRegion destinationRegion, Double travelTime,
                Double distanceKm, String roadName, String accessibilityStatus) {
        this.sourceRegion = sourceRegion;
        this.destinationRegion = destinationRegion;
        this.travelTime = travelTime;
        this.distanceKm = distanceKm;
        this.roadName = roadName;
        this.accessibilityStatus = accessibilityStatus;
    }

    // Getters and Setters
    public Integer getRoadId() {
        return roadId;
    }

    public void setRoadId(Integer roadId) {
        this.roadId = roadId;
    }

    public AffectedRegion getSourceRegion() {
        return sourceRegion;
    }

    public void setSourceRegion(AffectedRegion sourceRegion) {
        this.sourceRegion = sourceRegion;
    }

    public AffectedRegion getDestinationRegion() {
        return destinationRegion;
    }

    public void setDestinationRegion(AffectedRegion destinationRegion) {
        this.destinationRegion = destinationRegion;
    }

    public Double getTravelTime() {
        return travelTime;
    }

    public void setTravelTime(Double travelTime) {
        this.travelTime = travelTime;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public String getRoadName() {
        return roadName;
    }

    public void setRoadName(String roadName) {
        this.roadName = roadName;
    }

    public String getAccessibilityStatus() {
        return accessibilityStatus;
    }

    public void setAccessibilityStatus(String accessibilityStatus) {
        this.accessibilityStatus = accessibilityStatus;
    }
}

