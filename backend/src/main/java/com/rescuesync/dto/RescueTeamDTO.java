package com.rescuesync.dto;

import java.io.Serializable;

/**
 * Data Transfer Object for rescue team status and deployments.
 */
public class RescueTeamDTO implements Serializable {

    private Integer teamId;
    private String teamName;
    private String availability;
    private Integer assignedRegionId;
    private String assignedRegionName;

    public RescueTeamDTO() {
    }

    public RescueTeamDTO(Integer teamId, String teamName, String availability, Integer assignedRegionId, String assignedRegionName) {
        this.teamId = teamId;
        this.teamName = teamName;
        this.availability = availability;
        this.assignedRegionId = assignedRegionId;
        this.assignedRegionName = assignedRegionName;
    }

    // Getters and Setters
    public Integer getTeamId() {
        return teamId;
    }

    public void setTeamId(Integer teamId) {
        this.teamId = teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public Integer getAssignedRegionId() {
        return assignedRegionId;
    }

    public void setAssignedRegionId(Integer assignedRegionId) {
        this.assignedRegionId = assignedRegionId;
    }

    public String getAssignedRegionName() {
        return assignedRegionName;
    }

    public void setAssignedRegionName(String assignedRegionName) {
        this.assignedRegionName = assignedRegionName;
    }
}

