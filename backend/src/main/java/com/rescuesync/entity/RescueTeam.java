package com.rescuesync.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

/**
 * JPA Entity for 'rescue_teams' table.
 */
@Entity
@Table(name = "rescue_teams")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RescueTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_id")
    private Integer teamId;

    @NotBlank
    @Size(max = 100)
    @Column(name = "team_name", nullable = false, unique = true, length = 100)
    private String teamName;

    @NotBlank
    @Size(max = 20)
    @Column(name = "availability", nullable = false, length = 20)
    private String availability = "AVAILABLE";

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_region_id", nullable = true)
    private AffectedRegion assignedRegion;

    public RescueTeam() {
    }

    public RescueTeam(String teamName, String availability, AffectedRegion assignedRegion) {
        this.teamName = teamName;
        this.availability = availability;
        this.assignedRegion = assignedRegion;
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

    public AffectedRegion getAssignedRegion() {
        return assignedRegion;
    }

    public void setAssignedRegion(AffectedRegion assignedRegion) {
        this.assignedRegion = assignedRegion;
    }
}

