package com.rescuesync.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health check REST controller for RescueSync backend.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> checkHealth() {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("application", "RescueSync Backend");
        return ResponseEntity.ok(response);
    }
}

