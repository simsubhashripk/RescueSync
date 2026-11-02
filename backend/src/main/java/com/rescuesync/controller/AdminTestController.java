package com.rescuesync.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller with admin-restricted endpoints for verifying Role-Based Access Control (RBAC).
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminTestController {

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATOR')")
    public ResponseEntity<Map<String, String>> getAdminStatus() {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("status", "AUTHORIZED");
        response.put("access", "ADMIN_ONLY");
        response.put("message", "Administrative access verified.");
        return ResponseEntity.ok(response);
    }
}

