package com.rescuesync.repository;

import com.rescuesync.entity.AffectedRegion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AffectedRegionRepository extends JpaRepository<AffectedRegion, Integer> {
    Optional<AffectedRegion> findByRegionNameIgnoreCase(String regionName);
    boolean existsByRegionNameIgnoreCase(String regionName);
    List<AffectedRegion> findAllByOrderByPriorityScoreDesc();
}


