package com.rescuesync.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * JPA Entity for 'affected_regions' table.
 */
@Entity
@Table(name = "affected_regions")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AffectedRegion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "region_id")
    private Integer regionId;

    @NotBlank
    @Size(max = 100)
    @Column(name = "region_name", nullable = false, unique = true, length = 100)
    private String regionName;

    @NotNull
    @PositiveOrZero
    @Column(name = "population_affected", nullable = false)
    private Integer populationAffected;

    @NotNull
    @Min(1)
    @Max(10)
    @Column(name = "disaster_severity", nullable = false)
    private Integer disasterSeverity;

    @NotNull
    @PositiveOrZero
    @Column(name = "injured_people", nullable = false)
    private Integer injuredPeople = 0;

    @NotNull
    @PositiveOrZero
    @Column(name = "vulnerable_people", nullable = false)
    private Integer vulnerablePeople = 0;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    @Column(name = "road_accessibility", nullable = false)
    private Double roadAccessibility = 1.0;

    @NotBlank
    @Size(max = 100)
    @Column(name = "location", nullable = false, length = 100)
    private String location;

    @NotNull
    @Column(name = "priority_score", nullable = false)
    private Double priorityScore = 0.0;

    @Column(name = "latitude", nullable = false)
    private Double latitude = 0.0;

    @Column(name = "longitude", nullable = false)
    private Double longitude = 0.0;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    public AffectedRegion() {
    }

    public AffectedRegion(String regionName, Integer populationAffected, Integer disasterSeverity,
                          Integer injuredPeople, Integer vulnerablePeople, Double roadAccessibility,
                          String location, Double priorityScore, Double latitude, Double longitude) {
        this.regionName = regionName;
        this.populationAffected = populationAffected;
        this.disasterSeverity = disasterSeverity;
        this.injuredPeople = injuredPeople;
        this.vulnerablePeople = vulnerablePeople;
        this.roadAccessibility = roadAccessibility;
        this.location = location;
        this.priorityScore = priorityScore;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters and Setters
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

    public Integer getPopulationAffected() {
        return populationAffected;
    }

    public void setPopulationAffected(Integer populationAffected) {
        this.populationAffected = populationAffected;
    }

    public Integer getDisasterSeverity() {
        return disasterSeverity;
    }

    public void setDisasterSeverity(Integer disasterSeverity) {
        this.disasterSeverity = disasterSeverity;
    }

    public Integer getInjuredPeople() {
        return injuredPeople;
    }

    public void setInjuredPeople(Integer injuredPeople) {
        this.injuredPeople = injuredPeople;
    }

    public Integer getVulnerablePeople() {
        return vulnerablePeople;
    }

    public void setVulnerablePeople(Integer vulnerablePeople) {
        this.vulnerablePeople = vulnerablePeople;
    }

    public Double getRoadAccessibility() {
        return roadAccessibility;
    }

    public void setRoadAccessibility(Double roadAccessibility) {
        this.roadAccessibility = roadAccessibility;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Double getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(Double priorityScore) {
        this.priorityScore = priorityScore;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

