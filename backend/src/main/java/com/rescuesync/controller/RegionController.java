package com.rescuesync.controller;

import com.rescuesync.dto.MessageResponse;
import com.rescuesync.dto.RegionRequestDTO;
import com.rescuesync.dto.RegionResponseDTO;
import com.rescuesync.service.RegionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/regions")
public class RegionController {

    private final RegionService regionService;

    public RegionController(RegionService regionService) {
        this.regionService = regionService;
    }

    /**
     * Retrieve all disaster-affected regions.
     * Accessible to all authenticated users (USER, ADMIN, ADMINISTRATOR).
     */
    @GetMapping
    public ResponseEntity<List<RegionResponseDTO>> getAllRegions() {
        List<RegionResponseDTO> regions = regionService.getAllRegions();
        return ResponseEntity.ok(regions);
    }

    /**
     * Retrieve details for a specific disaster-affected region.
     * Accessible to all authenticated users.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RegionResponseDTO> getRegionById(@PathVariable Integer id) {
        RegionResponseDTO region = regionService.getRegionById(id);
        return ResponseEntity.ok(region);
    }

    /**
     * Register a new disaster-affected region.
     * Restricted to ADMIN and ADMINISTRATOR roles.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATOR')")
    public ResponseEntity<RegionResponseDTO> createRegion(@Valid @RequestBody RegionRequestDTO request) {
        RegionResponseDTO created = regionService.createRegion(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing disaster-affected region.
     * Restricted to ADMIN and ADMINISTRATOR roles.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATOR')")
    public ResponseEntity<RegionResponseDTO> updateRegion(@PathVariable Integer id,
                                                          @Valid @RequestBody RegionRequestDTO request) {
        RegionResponseDTO updated = regionService.updateRegion(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Safely delete a disaster-affected region if no foreign-key dependencies exist.
     * Restricted to ADMIN and ADMINISTRATOR roles.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATOR')")
    public ResponseEntity<MessageResponse> deleteRegion(@PathVariable Integer id) {
        regionService.deleteRegion(id);
        return ResponseEntity.ok(MessageResponse.success("Region ID " + id + " has been successfully removed."));
    }
}

