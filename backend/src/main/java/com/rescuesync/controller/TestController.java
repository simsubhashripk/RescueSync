package com.rescuesync.controller;

import com.rescuesync.entity.AffectedRegion;
import com.rescuesync.entity.ReliefRequest;
import com.rescuesync.entity.Resource;
import com.rescuesync.repository.AffectedRegionRepository;
import com.rescuesync.repository.ReliefRequestRepository;
import com.rescuesync.repository.ResourceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Phase 2 Database Verification Controller.
 * Exposes test endpoints returning actual database records from rescuesync_db.
 */
@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class TestController {

    private final AffectedRegionRepository regionRepository;
    private final ResourceRepository resourceRepository;
    private final ReliefRequestRepository requestRepository;

    public TestController(AffectedRegionRepository regionRepository,
                          ResourceRepository resourceRepository,
                          ReliefRequestRepository requestRepository) {
        this.regionRepository = regionRepository;
        this.resourceRepository = resourceRepository;
        this.requestRepository = requestRepository;
    }

    @GetMapping("/regions")
    public ResponseEntity<List<AffectedRegion>> getTestRegions() {
        List<AffectedRegion> regions = regionRepository.findAll();
        return ResponseEntity.ok(regions);
    }

    @GetMapping("/resources")
    public ResponseEntity<List<Resource>> getTestResources() {
        List<Resource> resources = resourceRepository.findAll();
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/requests")
    public ResponseEntity<List<ReliefRequest>> getTestRequests() {
        List<ReliefRequest> requests = requestRepository.findAll();
        return ResponseEntity.ok(requests);
    }
}

