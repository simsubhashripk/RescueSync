package com.rescuesync.repository;

import com.rescuesync.entity.RescueTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RescueTeamRepository extends JpaRepository<RescueTeam, Integer> {
    List<RescueTeam> findByAvailability(String availability);
    List<RescueTeam> findByAvailabilityIgnoreCase(String availability);
    List<RescueTeam> findByAssignedRegionRegionId(Integer assignedRegionId);
    List<RescueTeam> findAllByOrderByTeamIdAsc();
    long countByAvailabilityIgnoreCase(String availability);
    boolean existsByAssignedRegionRegionId(Integer assignedRegionId);
}

