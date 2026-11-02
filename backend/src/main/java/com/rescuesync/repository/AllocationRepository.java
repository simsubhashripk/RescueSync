package com.rescuesync.repository;

import com.rescuesync.entity.Allocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AllocationRepository extends JpaRepository<Allocation, Integer> {
    List<Allocation> findByRegionRegionId(Integer regionId);
    List<Allocation> findByRegionRegionIdOrderByAllocationIdDesc(Integer regionId);
    List<Allocation> findByRequestRequestIdOrderByAllocationIdDesc(Integer requestId);
    List<Allocation> findAllByOrderByAllocationIdDesc();
    boolean existsByRegionRegionId(Integer regionId);
    boolean existsBySupplyRegionRegionId(Integer supplyRegionId);
    boolean existsByRegionRegionIdAndResourceNameIgnoreCase(Integer regionId, String resourceName);
    boolean existsBySupplyRegionRegionIdAndResourceNameIgnoreCase(Integer supplyRegionId, String resourceName);
    boolean existsByRequestRequestId(Integer requestId);

    @Query("SELECT COALESCE(SUM(a.quantityAllocated), 0) FROM Allocation a")
    Long sumTotalUnitsAllocated();
}
