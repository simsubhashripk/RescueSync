package com.rescuesync.controller;

import com.rescuesync.dto.MessageResponse;
import com.rescuesync.dto.QueueSummaryDTO;
import com.rescuesync.dto.ReliefRequestCreateDTO;
import com.rescuesync.dto.ReliefRequestResponseDTO;
import com.rescuesync.service.ReliefRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
public class ReliefRequestController {

    private final ReliefRequestService reliefRequestService;

    public ReliefRequestController(ReliefRequestService reliefRequestService) {
        this.reliefRequestService = reliefRequestService;
    }

    /**
     * Retrieve all relief requests across all sectors.
     * Accessible to all authenticated users (USER, ADMIN, ADMINISTRATOR).
     */
    @GetMapping
    public ResponseEntity<List<ReliefRequestResponseDTO>> getAllRequests() {
        List<ReliefRequestResponseDTO> requests = reliefRequestService.getAllRequests();
        return ResponseEntity.ok(requests);
    }

    /**
     * Retrieve active PriorityQueue triage snapshot and summary statistics.
     * Accessible to all authenticated users.
     */
    @GetMapping("/queue")
    public ResponseEntity<QueueSummaryDTO> getPriorityQueueSummary() {
        QueueSummaryDTO summary = reliefRequestService.getPriorityQueueSummary();
        return ResponseEntity.ok(summary);
    }

    /**
     * Retrieve a specific relief request by ID.
     * Accessible to all authenticated users.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReliefRequestResponseDTO> getRequestById(@PathVariable Integer id) {
        ReliefRequestResponseDTO request = reliefRequestService.getRequestById(id);
        return ResponseEntity.ok(request);
    }

    /**
     * Retrieve all relief requests for a particular disaster region.
     * Accessible to all authenticated users.
     */
    @GetMapping("/region/{regionId}")
    public ResponseEntity<List<ReliefRequestResponseDTO>> getRequestsByRegion(@PathVariable Integer regionId) {
        List<ReliefRequestResponseDTO> requests = reliefRequestService.getRequestsByRegion(regionId);
        return ResponseEntity.ok(requests);
    }

    /**
     * Register a new relief demand for a disaster sector.
     * Accessible to all authenticated officers and administrators.
     */
    @PostMapping
    public ResponseEntity<ReliefRequestResponseDTO> createReliefRequest(@Valid @RequestBody ReliefRequestCreateDTO requestDTO) {
        ReliefRequestResponseDTO created = reliefRequestService.createReliefRequest(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Delete a relief request if no historical allocations reference it.
     * Restricted to ADMIN and ADMINISTRATOR roles.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATOR')")
    public ResponseEntity<MessageResponse> deleteReliefRequest(@PathVariable Integer id) {
        reliefRequestService.deleteReliefRequest(id);
        return ResponseEntity.ok(MessageResponse.success("Relief request #" + id + " has been successfully removed."));
    }
}

