package com.rescuesync.repository;

import com.rescuesync.entity.Road;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoadRepository extends JpaRepository<Road, Integer> {
    List<Road> findByAccessibilityStatus(String accessibilityStatus);
    List<Road> findBySourceRegionRegionId(Integer sourceRegionId);
    boolean existsBySourceRegionRegionIdOrDestinationRegionRegionId(Integer sourceRegionId, Integer destinationRegionId);
}

