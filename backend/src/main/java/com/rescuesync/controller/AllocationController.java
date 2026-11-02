package com.rescuesync.controller;

import com.rescuesync.dto.AllocationResponseDTO;
import com.rescuesync.dto.AllocationResultDTO;
import com.rescuesync.dto.AllocationSummaryDTO;
import com.rescuesync.dto.BatchAllocationResultDTO;
import com.rescuesync.service.AllocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/allocations")
public class AllocationController {

    private final AllocationService allocationService;

    public AllocationController(AllocationService allocationService) {
        this.allocationService = allocationService;
    }

    /**
     * Retrieve all completed allocation records.
     * Accessible to all authenticated users.
     */
    @GetMapping
    public ResponseEntity<List<AllocationResponseDTO>> getAllAllocations() {
        List<AllocationResponseDTO> list = allocationService.getAllAllocations();
        return ResponseEntity.ok(list);
    }

    /**
     * Retrieve allocation metrics and resource distribution summary.
     * Accessible to all authenticated users.
     */
    @GetMapping("/summary")
    public ResponseEntity<AllocationSummaryDTO> getAllocationSummary() {
        AllocationSummaryDTO summary = allocationService.getAllocationSummary();
        return ResponseEntity.ok(summary);
    }

    /**
     * Retrieve allocation history for a specific relief request.
     * Accessible to all authenticated users.
     */
    @GetMapping("/request/{requestId}")
    public ResponseEntity<List<AllocationResponseDTO>> getAllocationsByRequestId(@PathVariable Integer requestId) {
        List<AllocationResponseDTO> list = allocationService.getAllocationsByRequestId(requestId);
        return ResponseEntity.ok(list);
    }

    /**
     * Retrieve allocation history for a specific disaster region.
     * Accessible to all authenticated users.
     */
    @GetMapping("/region/{regionId}")
    public ResponseEntity<List<AllocationResponseDTO>> getAllocationsByRegionId(@PathVariable Integer regionId) {
        List<AllocationResponseDTO> list = allocationService.getAllocationsByRegionId(regionId);
        return ResponseEntity.ok(list);
    }

    /**
     * Allocate resources to a single relief request.
     * Restricted to administrators.
     */
    @PostMapping("/request/{requestId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATOR')")
    public ResponseEntity<AllocationResultDTO> allocateRequest(
            @PathVariable Integer requestId,
            Authentication authentication) {
        String allocatedBy = (authentication != null && authentication.getName() != null)
                ? authentication.getName()
                : "ADMIN";
        AllocationResultDTO result = allocationService.allocateRequest(requestId, allocatedBy);
        return ResponseEntity.ok(result);
    }

    /**
     * Execute batch allocation sequentially across the active priority queue.
     * Restricted to administrators.
     */
    @PostMapping("/execute")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATOR')")
    public ResponseEntity<BatchAllocationResultDTO> executeBatchAllocation(Authentication authentication) {
        String allocatedBy = (authentication != null && authentication.getName() != null)
                ? authentication.getName()
                : "ADMIN";
        BatchAllocationResultDTO result = allocationService.executeBatchAllocation(allocatedBy);
        return ResponseEntity.ok(result);
    }
}

