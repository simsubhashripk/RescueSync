package com.rescuesync.controller;

import com.rescuesync.dto.AdminDashboardSummaryDTO;
import com.rescuesync.dto.RegionAnalyticsDTO;
import com.rescuesync.dto.RescueTeamDTO;
import com.rescuesync.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for Phase 10 Admin Dashboard KPIs, Region Analytics, and Rescue Team Operations.
 */
@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Executive KPI summary for dashboard widgets.
     */
    @GetMapping("/summary")
    public ResponseEntity<AdminDashboardSummaryDTO> getDashboardSummary() {
        return ResponseEntity.ok(analyticsService.getAdminDashboardSummary());
    }

    /**
     * Region-wise composite analytics (disaster statistics, inventory, deficits, teams).
     */
    @GetMapping("/regions")
    public ResponseEntity<List<RegionAnalyticsDTO>> getRegionAnalytics() {
        return ResponseEntity.ok(analyticsService.getRegionAnalytics());
    }

    /**
     * Status of all rescue teams (Available, Deployed, Unassigned).
     */
    @GetMapping("/teams")
    public ResponseEntity<List<RescueTeamDTO>> getAllRescueTeams() {
        return ResponseEntity.ok(analyticsService.getAllRescueTeams());
    }

    /**
     * Dedicated admin authorization check endpoint for dashboard management privileges.
     */
    @GetMapping("/admin-check")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATOR')")
    public ResponseEntity<String> checkAdminPrivileges() {
        return ResponseEntity.ok("ADMIN_AUTHORIZED");
    }
}

