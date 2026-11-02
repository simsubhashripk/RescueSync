package com.rescuesync.dto;

import java.io.Serializable;

/**
 * High-level system overview KPIs for the Phase 10 Admin Dashboard.
 * Calculated dynamically from real database entities.
 */
public class AdminDashboardSummaryDTO implements Serializable {

    private long totalAffectedRegions;
    private long totalAvailableResources;
    private long totalReservedResources;
    private long totalPendingRequests;
    private long totalAllocatedRequests;
    private long totalCompletedAllocations;
    private long totalAllocatedUnits;
    private long totalRescueTeams;
    private long deployedRescueTeams;
    private long availableRescueTeams;
    private long totalRedistributionOperations;
    private long completedRedistributions;
    private long recommendedRedistributions;
    private long totalRedistributedUnits;
    private String overallDisasterStatus;
    private String highestSeverityRegion;
    private int highestSeverityLevel;
    private long totalPopulationAffected;

    public AdminDashboardSummaryDTO() {
    }

    // Getters and Setters
    public long getTotalAffectedRegions() {
        return totalAffectedRegions;
    }

    public void setTotalAffectedRegions(long totalAffectedRegions) {
        this.totalAffectedRegions = totalAffectedRegions;
    }

    public long getTotalAvailableResources() {
        return totalAvailableResources;
    }

    public void setTotalAvailableResources(long totalAvailableResources) {
        this.totalAvailableResources = totalAvailableResources;
    }

    public long getTotalReservedResources() {
        return totalReservedResources;
    }

    public void setTotalReservedResources(long totalReservedResources) {
        this.totalReservedResources = totalReservedResources;
    }

    public long getTotalPendingRequests() {
        return totalPendingRequests;
    }

    public void setTotalPendingRequests(long totalPendingRequests) {
        this.totalPendingRequests = totalPendingRequests;
    }

    public long getTotalAllocatedRequests() {
        return totalAllocatedRequests;
    }

    public void setTotalAllocatedRequests(long totalAllocatedRequests) {
        this.totalAllocatedRequests = totalAllocatedRequests;
    }

    public long getTotalCompletedAllocations() {
        return totalCompletedAllocations;
    }

    public void setTotalCompletedAllocations(long totalCompletedAllocations) {
        this.totalCompletedAllocations = totalCompletedAllocations;
    }

    public long getTotalAllocatedUnits() {
        return totalAllocatedUnits;
    }

    public void setTotalAllocatedUnits(long totalAllocatedUnits) {
        this.totalAllocatedUnits = totalAllocatedUnits;
    }

    public long getTotalRescueTeams() {
        return totalRescueTeams;
    }

    public void setTotalRescueTeams(long totalRescueTeams) {
        this.totalRescueTeams = totalRescueTeams;
    }

    public long getDeployedRescueTeams() {
        return deployedRescueTeams;
    }

    public void setDeployedRescueTeams(long deployedRescueTeams) {
        this.deployedRescueTeams = deployedRescueTeams;
    }

    public long getAvailableRescueTeams() {
        return availableRescueTeams;
    }

    public void setAvailableRescueTeams(long availableRescueTeams) {
        this.availableRescueTeams = availableRescueTeams;
    }

    public long getTotalRedistributionOperations() {
        return totalRedistributionOperations;
    }

    public void setTotalRedistributionOperations(long totalRedistributionOperations) {
        this.totalRedistributionOperations = totalRedistributionOperations;
    }

    public long getCompletedRedistributions() {
        return completedRedistributions;
    }

    public void setCompletedRedistributions(long completedRedistributions) {
        this.completedRedistributions = completedRedistributions;
    }

    public long getRecommendedRedistributions() {
        return recommendedRedistributions;
    }

    public void setRecommendedRedistributions(long recommendedRedistributions) {
        this.recommendedRedistributions = recommendedRedistributions;
    }

    public long getTotalRedistributedUnits() {
        return totalRedistributedUnits;
    }

    public void setTotalRedistributedUnits(long totalRedistributedUnits) {
        this.totalRedistributedUnits = totalRedistributedUnits;
    }

    public String getOverallDisasterStatus() {
        return overallDisasterStatus;
    }

    public void setOverallDisasterStatus(String overallDisasterStatus) {
        this.overallDisasterStatus = overallDisasterStatus;
    }

    public String getHighestSeverityRegion() {
        return highestSeverityRegion;
    }

    public void setHighestSeverityRegion(String highestSeverityRegion) {
        this.highestSeverityRegion = highestSeverityRegion;
    }

    public int getHighestSeverityLevel() {
        return highestSeverityLevel;
    }

    public void setHighestSeverityLevel(int highestSeverityLevel) {
        this.highestSeverityLevel = highestSeverityLevel;
    }

    public long getTotalPopulationAffected() {
        return totalPopulationAffected;
    }

    public void setTotalPopulationAffected(long totalPopulationAffected) {
        this.totalPopulationAffected = totalPopulationAffected;
    }
}

