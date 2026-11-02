package com.rescuesync.repository;

import com.rescuesync.entity.Shelter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShelterRepository extends JpaRepository<Shelter, Integer> {
    List<Shelter> findByRegionRegionId(Integer regionId);
    boolean existsByRegionRegionId(Integer regionId);
}

