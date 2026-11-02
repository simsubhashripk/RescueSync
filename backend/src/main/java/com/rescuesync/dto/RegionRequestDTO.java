package com.rescuesync.dto;

import jakarta.validation.constraints.*;

public class RegionRequestDTO {

    @NotBlank(message = "Region name must not be empty.")
    @Size(max = 100, message = "Region name must not exceed 100 characters.")
    private String regionName;

    @NotBlank(message = "Location description must not be empty.")
    @Size(max = 100, message = "Location must not exceed 100 characters.")
    private String location;

    @NotNull(message = "Population affected is required.")
    @Min(value = 0, message = "Population affected must be non-negative.")
    private Integer populationAffected;

    @NotNull(message = "Disaster severity is required.")
    @Min(value = 1, message = "Disaster severity must be between 1 and 10.")
    @Max(value = 10, message = "Disaster severity must be between 1 and 10.")
    private Integer disasterSeverity;

    @NotNull(message = "Injured people count is required.")
    @Min(value = 0, message = "Injured people count must be non-negative.")
    private Integer injuredPeople;

    @NotNull(message = "Vulnerable people count is required.")
    @Min(value = 0, message = "Vulnerable people count must be non-negative.")
    private Integer vulnerablePeople;

    @NotNull(message = "Road accessibility is required.")
    @DecimalMin(value = "0.0", message = "Road accessibility must be between 0.0 (completely inaccessible) and 1.0 (fully accessible).")
    @DecimalMax(value = "1.0", message = "Road accessibility must be between 0.0 (completely inaccessible) and 1.0 (fully accessible).")
    private Double roadAccessibility;

    @NotNull(message = "Latitude is required.")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90.0 and 90.0.")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90.0 and 90.0.")
    private Double latitude;

    @NotNull(message = "Longitude is required.")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180.0 and 180.0.")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180.0 and 180.0.")
    private Double longitude;

    public RegionRequestDTO() {
    }

    public RegionRequestDTO(String regionName, String location, Integer populationAffected,
                            Integer disasterSeverity, Integer injuredPeople, Integer vulnerablePeople,
                            Double roadAccessibility, Double latitude, Double longitude) {
        this.regionName = regionName;
        this.location = location;
        this.populationAffected = populationAffected;
        this.disasterSeverity = disasterSeverity;
        this.injuredPeople = injuredPeople;
        this.vulnerablePeople = vulnerablePeople;
        this.roadAccessibility = roadAccessibility;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters and Setters
    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
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
}

