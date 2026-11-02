package com.rescuesync.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

/**
 * JPA Entity for 'shelters' table.
 */
@Entity
@Table(name = "shelters")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Shelter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shelter_id")
    private Integer shelterId;

    @NotBlank
    @Size(max = 100)
    @Column(name = "shelter_name", nullable = false, length = 100)
    private String shelterName;

    @NotBlank
    @Size(max = 100)
    @Column(name = "location", nullable = false, length = 100)
    private String location;

    @NotNull
    @Positive
    @Column(name = "total_capacity", nullable = false)
    private Integer totalCapacity;

    @NotNull
    @PositiveOrZero
    @Column(name = "occupied_capacity", nullable = false)
    private Integer occupiedCapacity = 0;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "region_id", nullable = true)
    private AffectedRegion region;

    public Shelter() {
    }

    public Shelter(String shelterName, String location, Integer totalCapacity, Integer occupiedCapacity, AffectedRegion region) {
        this.shelterName = shelterName;
        this.location = location;
        this.totalCapacity = totalCapacity;
        this.occupiedCapacity = occupiedCapacity;
        this.region = region;
    }

    // Getters and Setters
    public Integer getShelterId() {
        return shelterId;
    }

    public void setShelterId(Integer shelterId) {
        this.shelterId = shelterId;
    }

    public String getShelterName() {
        return shelterName;
    }

    public void setShelterName(String shelterName) {
        this.shelterName = shelterName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(Integer totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public Integer getOccupiedCapacity() {
        return occupiedCapacity;
    }

    public void setOccupiedCapacity(Integer occupiedCapacity) {
        this.occupiedCapacity = occupiedCapacity;
    }

    public AffectedRegion getRegion() {
        return region;
    }

    public void setRegion(AffectedRegion region) {
        this.region = region;
    }
}

