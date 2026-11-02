package com.rescuesync;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.rescuesync.dto.AdminDashboardSummaryDTO;
import com.rescuesync.dto.RegionAnalyticsDTO;
import com.rescuesync.dto.RescueTeamDTO;
import com.rescuesync.repository.AffectedRegionRepository;
import com.rescuesync.repository.AllocationRepository;
import com.rescuesync.repository.RedistributionRecordRepository;
import com.rescuesync.repository.RescueTeamRepository;
import com.rescuesync.repository.ResourceRepository;
import com.rescuesync.service.AnalyticsService;

@SpringBootTest
@ActiveProfiles("default")
@Transactional
public class AdminAnalyticsTests {

@Autowired
private AnalyticsService analyticsService;

@Autowired
private ResourceRepository resourceRepository;

@Autowired
private AffectedRegionRepository affectedRegionRepository;

@Autowired
private AllocationRepository allocationRepository;

@Autowired
private RedistributionRecordRepository redistributionRecordRepository;

@Autowired
private RescueTeamRepository rescueTeamRepository;

@Test
@DisplayName("Test 1: Admin dashboard summary accurately aggregates real database records")
void testAdminDashboardSummaryMatchesBaselineData() {

    AdminDashboardSummaryDTO summary =
            analyticsService.getAdminDashboardSummary();

    assertNotNull(summary);

    assertEquals(
            affectedRegionRepository.count(),
            summary.getTotalAffectedRegions(),
            "Must match affected regions count"
    );

   long expectedAvailable = resourceRepository.findAll().stream()
        .map(r -> r.getAvailableQuantity())
        .filter(value -> value != null)
        .mapToLong(Integer::longValue)
        .sum();

long expectedReserved = resourceRepository.findAll().stream()
        .map(r -> r.getReservedQuantity())
        .filter(value -> value != null)
        .mapToLong(Integer::longValue)
        .sum();

    assertEquals(
            expectedAvailable,
            summary.getTotalAvailableResources(),
            "Must match actual available units in database"
    );

    assertEquals(
            expectedReserved,
            summary.getTotalReservedResources(),
            "Must match actual reserved units in database"
    );

    assertEquals(
            allocationRepository.count(),
            summary.getTotalCompletedAllocations(),
            "Must match allocation records count"
    );

    assertEquals(
            rescueTeamRepository.count(),
            summary.getTotalRescueTeams()
    );

    assertEquals(
            rescueTeamRepository.countByAvailabilityIgnoreCase("DEPLOYED"),
            summary.getDeployedRescueTeams()
    );

    assertEquals(
            rescueTeamRepository.countByAvailabilityIgnoreCase("AVAILABLE"),
            summary.getAvailableRescueTeams()
    );

    assertEquals(
            redistributionRecordRepository.countByStatus("COMPLETED"),
            summary.getCompletedRedistributions()
    );

    assertEquals(9, summary.getHighestSeverityLevel());
    assertEquals("Sector A (Coastal Zone)", summary.getHighestSeverityRegion());
    assertEquals("CRITICAL - ACTIVE RELIEF", summary.getOverallDisasterStatus());

    assertTrue(summary.getTotalPopulationAffected() > 0);
}

@Test
@DisplayName("Test 2: Region analytics generates statistics and team assignments for all sectors")
void testRegionAnalyticsCoversAllRegions() {

    List<RegionAnalyticsDTO> regionAnalytics =
            analyticsService.getRegionAnalytics();

    assertNotNull(regionAnalytics);
    assertEquals(4, regionAnalytics.size());

    RegionAnalyticsDTO sectorA = regionAnalytics.stream()
            .filter(r -> r.getRegionId() == 1)
            .findFirst()
            .orElse(null);

    assertNotNull(sectorA);

    assertEquals("Sector A (Coastal Zone)", sectorA.getRegionName());
    assertEquals(9, sectorA.getDisasterSeverity());

    assertTrue(
            sectorA.getPendingRequiredUnits() > 0,
            "Sector A has pending unfulfilled relief demands"
    );

    assertEquals(1, sectorA.getAssignedTeams().size());

    assertTrue(
            sectorA.getAssignedTeams().get(0)
                    .contains("Alpha Quick Response Unit")
    );

    assertTrue(
            sectorA.getAssignedTeams().get(0)
                    .contains("DEPLOYED")
    );
}

@Test
@DisplayName("Test 3: Rescue teams metadata accurately reflects database state")
void testRescueTeamsList() {

    List<RescueTeamDTO> teams =
            analyticsService.getAllRescueTeams();

    assertNotNull(teams);
    assertEquals(3, teams.size());

    RescueTeamDTO team1 = teams.stream()
            .filter(t -> t.getTeamId() == 1)
            .findFirst()
            .orElse(null);

    assertNotNull(team1);
    assertEquals("Alpha Quick Response Unit", team1.getTeamName());
    assertEquals("DEPLOYED", team1.getAvailability());
    assertEquals("Sector A (Coastal Zone)", team1.getAssignedRegionName());

    RescueTeamDTO team2 = teams.stream()
            .filter(t -> t.getTeamId() == 2)
            .findFirst()
            .orElse(null);

    assertNotNull(team2);
    assertEquals("Bravo Medical Evac", team2.getTeamName());
    assertEquals("AVAILABLE", team2.getAvailability());

    RescueTeamDTO team3 = teams.stream()
            .filter(t -> t.getTeamId() == 3)
            .findFirst()
            .orElse(null);

    assertNotNull(team3);
    assertEquals("Charlie Search & Rescue", team3.getTeamName());
    assertEquals("AVAILABLE", team3.getAvailability());
}


}
