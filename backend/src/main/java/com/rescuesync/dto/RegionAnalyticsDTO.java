package com.rescuesync.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Region-wise composite analytics combining disaster statistics, resource reserves,
 * pending relief deficits, fulfillment rates, and assigned rescue teams.
 */
public class RegionAnalyticsDTO implements Serializable {

    private Integer regionId;
    private String regionName;
    private String location;
    private Integer disasterSeverity;
    private Integer injuredPeople;
    private Integer vulnerablePeople;
    private Double roadAccessibility;
    private Double priorityScore;
    private Integer populationAffected;
    private Long totalAvailableStock;
    private Long totalReservedStock;
    private Long pendingRequiredUnits;
    private Long allocatedUnits;
    private Double fulfillmentPercentage;
    private List<String> assignedTeams = new ArrayList<>();

    public RegionAnalyticsDTO() {
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

    public Integer getPopulationAffected() {
        return populationAffected;
    }

    public void setPopulationAffected(Integer populationAffected) {
        this.populationAffected = populationAffected;
    }

    public Long getTotalAvailableStock() {
        return totalAvailableStock;
    }

    public void setTotalAvailableStock(Long totalAvailableStock) {
        this.totalAvailableStock = totalAvailableStock;
    }

    public Long getTotalReservedStock() {
        return totalReservedStock;
    }

    public void setTotalReservedStock(Long totalReservedStock) {
        this.totalReservedStock = totalReservedStock;
    }

    public Long getPendingRequiredUnits() {
        return pendingRequiredUnits;
    }

    public void setPendingRequiredUnits(Long pendingRequiredUnits) {
        this.pendingRequiredUnits = pendingRequiredUnits;
    }

    public Long getAllocatedUnits() {
        return allocatedUnits;
    }

    public void setAllocatedUnits(Long allocatedUnits) {
        this.allocatedUnits = allocatedUnits;
    }

    public Double getFulfillmentPercentage() {
        return fulfillmentPercentage;
    }

    public void setFulfillmentPercentage(Double fulfillmentPercentage) {
        this.fulfillmentPercentage = fulfillmentPercentage;
    }

    public List<String> getAssignedTeams() {
        return assignedTeams;
    }

    public void setAssignedTeams(List<String> assignedTeams) {
        this.assignedTeams = assignedTeams;
    }
}

