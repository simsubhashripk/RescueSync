package com.rescuesync.controller;

import com.rescuesync.dto.MessageResponse;
import com.rescuesync.dto.ResourceRequestDTO;
import com.rescuesync.dto.ResourceResponseDTO;
import com.rescuesync.service.ResourceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    /**
     * Retrieve all resource inventory records across all disaster sectors.
     * Accessible to all authenticated users (USER, ADMIN, ADMINISTRATOR).
     */
    @GetMapping
    public ResponseEntity<List<ResourceResponseDTO>> getAllResources() {
        List<ResourceResponseDTO> resources = resourceService.getAllResources();
        return ResponseEntity.ok(resources);
    }

    /**
     * Retrieve a specific resource by ID.
     * Accessible to all authenticated users.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResourceResponseDTO> getResourceById(@PathVariable Integer id) {
        ResourceResponseDTO resource = resourceService.getResourceById(id);
        return ResponseEntity.ok(resource);
    }

    /**
     * Retrieve all resources belonging to a particular disaster region.
     * Accessible to all authenticated users.
     */
    @GetMapping("/region/{regionId}")
    public ResponseEntity<List<ResourceResponseDTO>> getResourcesByRegion(@PathVariable Integer regionId) {
        List<ResourceResponseDTO> resources = resourceService.getResourcesByRegion(regionId);
        return ResponseEntity.ok(resources);
    }

    /**
     * Add a new resource item to a disaster region.
     * Restricted to ADMIN and ADMINISTRATOR roles.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATOR')")
    public ResponseEntity<ResourceResponseDTO> createResource(@Valid @RequestBody ResourceRequestDTO request) {
        ResourceResponseDTO created = resourceService.createResource(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing resource record's quantities or details.
     * Restricted to ADMIN and ADMINISTRATOR roles.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATOR')")
    public ResponseEntity<ResourceResponseDTO> updateResource(@PathVariable Integer id,
                                                              @Valid @RequestBody ResourceRequestDTO request) {
        ResourceResponseDTO updated = resourceService.updateResource(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Safely delete a resource inventory record if no dependencies exist.
     * Restricted to ADMIN and ADMINISTRATOR roles.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATOR')")
    public ResponseEntity<MessageResponse> deleteResource(@PathVariable Integer id) {
        resourceService.deleteResource(id);
        return ResponseEntity.ok(MessageResponse.success("Resource ID " + id + " has been successfully removed."));
    }
}

