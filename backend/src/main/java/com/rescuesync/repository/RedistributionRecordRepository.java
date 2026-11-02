package com.rescuesync.repository;

import com.rescuesync.entity.RedistributionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RedistributionRecordRepository extends JpaRepository<RedistributionRecord, Integer> {
    List<RedistributionRecord> findByStatusOrderByRecommendedAtDesc(String status);
    List<RedistributionRecord> findByStatusOrderByRecordIdDesc(String status);
    List<RedistributionRecord> findAllByOrderByRecordIdDesc();
    boolean existsBySourceRegionRegionIdOrDestRegionRegionId(Integer sourceRegionId, Integer destRegionId);
    boolean existsBySourceRegionRegionIdAndResourceNameIgnoreCase(Integer sourceRegionId, String resourceName);
    boolean existsByDestRegionRegionIdAndResourceNameIgnoreCase(Integer destRegionId, String resourceName);

    long countByStatus(String status);

    @Query("SELECT COALESCE(SUM(r.quantity), 0) FROM RedistributionRecord r WHERE r.status = :status")
    Integer sumQuantityByStatus(@Param("status") String status);
}
