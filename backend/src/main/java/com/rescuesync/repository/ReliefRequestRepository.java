package com.rescuesync.repository;

import com.rescuesync.entity.ReliefRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReliefRequestRepository extends JpaRepository<ReliefRequest, Integer> {
    List<ReliefRequest> findByRegionRegionId(Integer regionId);
    List<ReliefRequest> findByRegionRegionIdOrderByPriorityScoreDescRequestArrivalOrderAsc(Integer regionId);
    List<ReliefRequest> findByRequestStatusOrderByPriorityScoreDescRequestArrivalOrderAsc(String requestStatus);
    List<ReliefRequest> findByRequestStatusInOrderByPriorityScoreDescRequestArrivalOrderAsc(List<String> statuses);
    List<ReliefRequest> findAllByOrderByPriorityScoreDescRequestArrivalOrderAsc();
    boolean existsByRegionRegionId(Integer regionId);
    boolean existsByRegionRegionIdAndResourceRequiredIgnoreCase(Integer regionId, String resourceRequired);

    @Query("SELECT COALESCE(MAX(r.requestArrivalOrder), 0) FROM ReliefRequest r")
    Integer findMaxArrivalOrder();
}


