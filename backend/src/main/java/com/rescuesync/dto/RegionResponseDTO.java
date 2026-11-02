package com.rescuesync.dto;

import com.rescuesync.entity.AffectedRegion;
import java.time.LocalDateTime;

public class RegionResponseDTO {

    private Integer regionId;
    private String regionName;
    private String location;
    private Integer populationAffected;
    private Integer disasterSeverity;
    private Integer injuredPeople;
    private Integer vulnerablePeople;
    private Double roadAccessibility;
    private Double priorityScore;
    private Double latitude;
    private Double longitude;
    private LocalDateTime createdAt;

    public RegionResponseDTO() {
    }

    public static RegionResponseDTO fromEntity(AffectedRegion region) {
        if (region == null) return null;
        RegionResponseDTO dto = new RegionResponseDTO();
        dto.setRegionId(region.getRegionId());
        dto.setRegionName(region.getRegionName());
        dto.setLocation(region.getLocation());
        dto.setPopulationAffected(region.getPopulationAffected());
        dto.setDisasterSeverity(region.getDisasterSeverity());
        dto.setInjuredPeople(region.getInjuredPeople());
        dto.setVulnerablePeople(region.getVulnerablePeople());
        dto.setRoadAccessibility(region.getRoadAccessibility());
        dto.setPriorityScore(region.getPriorityScore());
        dto.setLatitude(region.getLatitude());
        dto.setLongitude(region.getLongitude());
        dto.setCreatedAt(region.getCreatedAt());
        return dto;
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

