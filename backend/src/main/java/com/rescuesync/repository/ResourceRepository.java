package com.rescuesync.repository;

import com.rescuesync.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Integer> {
    List<Resource> findByRegionRegionId(Integer regionId);
    List<Resource> findByRegionRegionIdOrderByResourceNameAsc(Integer regionId);
    List<Resource> findAllByOrderByRegionRegionNameAscResourceNameAsc();
    List<Resource> findByResourceNameIgnoreCase(String resourceName);
    List<Resource> findByResourceNameIgnoreCaseAndRegionRegionIdNot(String resourceName, Integer regionId);
    Optional<Resource> findByRegionRegionIdAndResourceNameIgnoreCase(Integer regionId, String resourceName);
    boolean existsByRegionRegionId(Integer regionId);
    boolean existsByRegionRegionIdAndResourceNameIgnoreCase(Integer regionId, String resourceName);
}
