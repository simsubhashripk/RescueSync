package com.rescuesync.controller;

import com.rescuesync.dto.*;
import com.rescuesync.service.RedistributionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/redistribution")
public class RedistributionController {

    private final RedistributionService redistributionService;

    public RedistributionController(RedistributionService redistributionService) {
        this.redistributionService = redistributionService;
    }

    /**
     * Detect and retrieve active resource shortages across all sectors.
     * Accessible to all authenticated users.
     */
    @GetMapping("/shortages")
    public ResponseEntity<List<ResourceAuditReportDTO>> getShortages() {
        List<ResourceAuditReportDTO> shortages = redistributionService.detectShortages();
        return ResponseEntity.ok(shortages);
    }

    /**
     * Detect and retrieve uncommitted resource surpluses across all sectors.
     * Accessible to all authenticated users.
     */
    @GetMapping("/surpluses")
    public ResponseEntity<List<ResourceAuditReportDTO>> getSurpluses() {
        List<ResourceAuditReportDTO> surpluses = redistributionService.detectSurpluses();
        return ResponseEntity.ok(surpluses);
    }

    /**
     * Retrieve active recommended transfers.
     * Accessible to all authenticated users.
     */
    @GetMapping("/recommendations")
    public ResponseEntity<List<RedistributionResponseDTO>> getRecommendations() {
        List<RedistributionResponseDTO> list = redistributionService.getActiveRecommendations();
        return ResponseEntity.ok(list);
    }

    /**
     * Generate route-verified redistribution recommendations.
     * Restricted to administrators. Does NOT modify stock inventory.
     */
    @PostMapping("/recommendations/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATOR')")
    public ResponseEntity<List<RedistributionResponseDTO>> generateRecommendations() {
        List<RedistributionResponseDTO> generated = redistributionService.generateRedistributionRecommendations();
        return ResponseEntity.ok(generated);
    }

    /**
     * Retrieve complete redistribution history.
     * Accessible to all authenticated users.
     */
    @GetMapping("/history")
    public ResponseEntity<List<RedistributionResponseDTO>> getHistory() {
        List<RedistributionResponseDTO> history = redistributionService.getAllRedistributionRecords();
        return ResponseEntity.ok(history);
    }

    /**
     * Retrieve redistribution summary metrics.
     * Accessible to all authenticated users.
     */
    @GetMapping("/summary")
    public ResponseEntity<RedistributionSummaryDTO> getSummary() {
        RedistributionSummaryDTO summary = redistributionService.getRedistributionSummary();
        return ResponseEntity.ok(summary);
    }

    /**
     * Approve and execute an individual recommended redistribution transfer.
     * Restricted to administrators.
     */
    @PostMapping("/execute/{recordId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATOR')")
    public ResponseEntity<RedistributionTransferResultDTO> executeTransfer(@PathVariable Integer recordId) {
        RedistributionTransferResultDTO result = redistributionService.executeTransfer(recordId);
        return ResponseEntity.ok(result);
    }

    /**
     * Approve and execute all pending recommended redistribution transfers in batch.
     * Restricted to administrators.
     */
    @PostMapping("/execute-batch")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATOR')")
    public ResponseEntity<List<RedistributionTransferResultDTO>> executeBatch() {
        List<RedistributionTransferResultDTO> results = redistributionService.executeBatchRedistribution();
        return ResponseEntity.ok(results);
    }
}

