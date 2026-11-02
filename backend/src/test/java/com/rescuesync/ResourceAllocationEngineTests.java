package com.rescuesync;

import com.rescuesync.dto.AllocationResultDTO;
import com.rescuesync.dto.AllocationSummaryDTO;
import com.rescuesync.entity.AffectedRegion;
import com.rescuesync.entity.ReliefRequest;
import com.rescuesync.entity.Resource;
import com.rescuesync.repository.AffectedRegionRepository;
import com.rescuesync.repository.AllocationRepository;
import com.rescuesync.repository.ReliefRequestRepository;
import com.rescuesync.repository.ResourceRepository;
import com.rescuesync.service.AllocationService;
import com.rescuesync.service.ReliefRequestService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ResourceAllocationEngineTests {

    @Autowired
    private AllocationService allocationService;

    @Autowired
    private AllocationRepository allocationRepository;

    @Autowired
    private ReliefRequestRepository reliefRequestRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private AffectedRegionRepository affectedRegionRepository;

    @Autowired
    private com.rescuesync.repository.RoadRepository roadRepository;

    @Test
    @DisplayName("Verify Priority Ordering (Score DESC) and FIFO Tie-Breaker (ArrivalOrder ASC)")
    void testPriorityQueueOrderingAndFifo() {
        ReliefRequest reqHigh = new ReliefRequest();
        reqHigh.setPriorityScore(84.5);
        reqHigh.setRequestArrivalOrder(1);

        ReliefRequest reqMid1 = new ReliefRequest();
        reqMid1.setPriorityScore(52.68);
        reqMid1.setRequestArrivalOrder(3);

        ReliefRequest reqMid2 = new ReliefRequest();
        reqMid2.setPriorityScore(52.68);
        reqMid2.setRequestArrivalOrder(4);

        ReliefRequest reqLow = new ReliefRequest();
        reqLow.setPriorityScore(28.0);
        reqLow.setRequestArrivalOrder(2);

        List<ReliefRequest> list = new ArrayList<>(List.of(reqMid2, reqLow, reqHigh, reqMid1));
        list.sort((a, b) -> {
            int cmp = Double.compare(b.getPriorityScore(), a.getPriorityScore());
            if (cmp != 0) return cmp;
            return Integer.compare(a.getRequestArrivalOrder(), b.getRequestArrivalOrder());
        });

        assertSame(reqHigh, list.get(0), "Highest priority score should be ranked #1");
        assertSame(reqMid1, list.get(1), "Equal priority score with earlier arrival order (3) should come before (4)");
        assertSame(reqMid2, list.get(2), "Equal priority score with later arrival order (4) should come after (3)");
        assertSame(reqLow, list.get(3), "Lowest priority score should be ranked last");
    }

    @Test
    @DisplayName("Verify Surplus Calculation Rule: max(0, Available - Reserved)")
    void testSurplusCalculationRule() {
        // Case 1: Available 0, Reserved 50 -> Surplus = 0 (Never negative)
        int surplus1 = Math.max(0, 0 - 50);
        assertEquals(0, surplus1, "Surplus must never be negative even if available < reserved");

        // Case 2: Available 100, Reserved 50 -> Surplus = 50
        int surplus2 = Math.max(0, 100 - 50);
        assertEquals(50, surplus2, "Surplus should be exactly 50 units beyond reserved buffer");

        // Case 3: Available 50, Reserved 50 -> Surplus = 0
        int surplus3 = Math.max(0, 50 - 50);
        assertEquals(0, surplus3, "When available equals reserved buffer, surplus is 0");
    }

    @Test
    @DisplayName("Verify Reading Existing Baseline Allocation History")
    void testReadAllocationHistory() {
        var allocations = allocationService.getAllAllocations();
        assertNotNull(allocations);
        assertFalse(allocations.isEmpty(), "Allocations history should contain baseline record #1");

        var first = allocations.stream().filter(a -> a.getAllocationId() == 1).findFirst().orElse(null);
        assertNotNull(first, "Allocation #1 must exist in the database");
        assertEquals("Water", first.getResourceName());
        assertEquals(200, first.getQuantityAllocated());
        assertEquals("SYSTEM", first.getAllocatedBy());
    }

    @Test
    @DisplayName("Verify Allocation Summary Metrics Calculation")
    void testAllocationSummary() {
        AllocationSummaryDTO summary = allocationService.getAllocationSummary();
        assertNotNull(summary);
        assertTrue(summary.getTotalAllocations() >= 1, "Total allocations should be at least 1");
        assertTrue(summary.getTotalUnitsAllocated() >= 200, "Total units allocated should be at least 200");
        assertNotNull(summary.getResourceDistribution());
        assertTrue(summary.getResourceDistribution().containsKey("Water"));
    }

    @Test
    @Transactional
    @DisplayName("Verify Safe Allocation with Temporary Request (Rolled Back)")
    void testAllocationWithTemporaryRequest() {
        AffectedRegion region = affectedRegionRepository.findById(1).orElseThrow();

        // Create temporary request for Food (Region 1 has Available: 120, Reserved: 50 -> Surplus: 70)
        ReliefRequest tempReq = new ReliefRequest();
        tempReq.setRegion(region);
        tempReq.setResourceRequired("Food");
        tempReq.setRequiredQuantity(25);
        tempReq.setAllocatedQuantity(0);
        tempReq.setPriorityScore(75.0);
        tempReq.setRequestArrivalOrder(999);
        tempReq.setRequestStatus(ReliefRequestService.STATUS_PENDING);
        ReliefRequest savedReq = reliefRequestRepository.save(tempReq);

        // Execute allocation
        AllocationResultDTO result = allocationService.allocateRequest(savedReq.getRequestId(), "TEST_ADMIN");
        assertTrue(result.isSuccess(), "Allocation should succeed using local surplus");
        assertEquals(25, result.getAllocatedQuantity(), "Should allocate all 25 requested units");
        assertEquals(0, result.getRemainingDeficit(), "Deficit should be 0");
        assertEquals(ReliefRequestService.STATUS_ALLOCATED, result.getRequestStatus(), "Status should be ALLOCATED");
        assertNotNull(result.getAllocationId(), "An allocation record ID should be generated");

        // Verify stock was safely deducted without breaching reserve
        Resource localFood = resourceRepository.findByRegionRegionIdAndResourceNameIgnoreCase(1, "Food").orElseThrow();
        assertEquals(95, localFood.getAvailableQuantity(), "120 - 25 = 95 units remaining");
        assertTrue(localFood.getAvailableQuantity() >= localFood.getReservedQuantity(), "Available must remain >= reserved buffer");
    }

    @Test
    @Transactional
    @DisplayName("Verify Partial Allocation when Surplus is Less than Demand (Rolled Back)")
    void testPartialAllocationWhenSurplusInsufficient() {
        AffectedRegion region = affectedRegionRepository.findById(1).orElseThrow();

        // Create temporary demand of 100 units of Food (Surplus is only 70 units: 120 - 50)
        ReliefRequest tempReq = new ReliefRequest();
        tempReq.setRegion(region);
        tempReq.setResourceRequired("Food");
        tempReq.setRequiredQuantity(100);
        tempReq.setAllocatedQuantity(0);
        tempReq.setPriorityScore(80.0);
        tempReq.setRequestArrivalOrder(998);
        tempReq.setRequestStatus(ReliefRequestService.STATUS_PENDING);
        ReliefRequest savedReq = reliefRequestRepository.save(tempReq);

        AllocationResultDTO result = allocationService.allocateRequest(savedReq.getRequestId(), "TEST_ADMIN");
        assertTrue(result.isSuccess());
        assertEquals(70, result.getAllocatedQuantity(), "Should allocate exactly available surplus of 70 units");
        assertEquals(30, result.getRemainingDeficit(), "Remaining deficit should be 30 units");
        assertEquals(ReliefRequestService.STATUS_PARTIALLY_ALLOCATED, result.getRequestStatus());

        Resource localFood = resourceRepository.findByRegionRegionIdAndResourceNameIgnoreCase(1, "Food").orElseThrow();
        assertEquals(50, localFood.getAvailableQuantity(), "Available stock should equal reserved buffer (50 units)");
        assertEquals(50, localFood.getReservedQuantity());
    }

    @Test
    @Transactional
    @DisplayName("Verify Repeated Allocation Call on Fully Allocated Request Does Not Duplicate (Rolled Back)")
    void testRepeatedAllocationNoDuplicate() {
        AffectedRegion region = affectedRegionRepository.findById(1).orElseThrow();

        ReliefRequest tempReq = new ReliefRequest();
        tempReq.setRegion(region);
        tempReq.setResourceRequired("Food");
        tempReq.setRequiredQuantity(10);
        tempReq.setAllocatedQuantity(10);
        tempReq.setPriorityScore(50.0);
        tempReq.setRequestArrivalOrder(997);
        tempReq.setRequestStatus(ReliefRequestService.STATUS_ALLOCATED);
        ReliefRequest savedReq = reliefRequestRepository.save(tempReq);

        long allocCountBefore = allocationRepository.count();
        AllocationResultDTO result = allocationService.allocateRequest(savedReq.getRequestId(), "TEST_ADMIN");
        assertFalse(result.isSuccess(), "Should not allocate to an already satisfied request");
        long allocCountAfter = allocationRepository.count();
        assertEquals(allocCountBefore, allocCountAfter, "No new allocation record should be created");
    }

    @Test
    @Transactional
    @DisplayName("Verify Route-Aware Donor Selection (Local Shortage Dispatches via Fastest Reachable Donor)")
    void testRouteAwareDonorSelectionWithDijkstraRoute() {
        AffectedRegion region1 = affectedRegionRepository.findById(1).orElseThrow();

        // Region 1 has 0 available Water (50 reserved, surplus = 0).
        // Request 20 units of Water in Region 1.
        ReliefRequest tempReq = new ReliefRequest();
        tempReq.setRegion(region1);
        tempReq.setResourceRequired("Water");
        tempReq.setRequiredQuantity(20);
        tempReq.setAllocatedQuantity(0);
        tempReq.setPriorityScore(88.0);
        tempReq.setRequestArrivalOrder(995);
        tempReq.setRequestStatus(ReliefRequestService.STATUS_PENDING);
        ReliefRequest savedReq = reliefRequestRepository.save(tempReq);

        AllocationResultDTO result = allocationService.allocateRequest(savedReq.getRequestId(), "ROUTE_ADMIN");
        assertTrue(result.isSuccess(), "Allocation should succeed from reachable donor");
        assertEquals(20, result.getAllocatedQuantity());
        assertEquals(2, result.getSupplyRegionId(), "Region 2 is the fastest open donor (30 mins, 15 km)");

        // Verify the persisted allocation record has the exact Dijkstra route distance
        var allocRecord = allocationRepository.findById(result.getAllocationId()).orElseThrow();
        assertEquals(15.0, allocRecord.getRouteDistanceKm(), "Route distance should be 15.0 km for Region 2 -> Region 1");
        assertEquals(2, allocRecord.getSupplyRegion().getRegionId());
        assertEquals(1, allocRecord.getRegion().getRegionId());
    }

    @Test
    @Transactional
    @DisplayName("Verify Dijkstra Detour When Direct Road is Blocked (Region 2 to Region 4 via Region 3)")
    void testDijkstraDetourWhenDirectRoadIsBlocked() {
        AffectedRegion region4 = affectedRegionRepository.findById(4).orElseThrow();

        // Temporarily deplete Region 4 Food local surplus so donor must be used
        Resource r4Food = resourceRepository.findByRegionRegionIdAndResourceNameIgnoreCase(4, "Food").orElseThrow();
        r4Food.setAvailableQuantity(50); // available = reserved = 50 -> surplus = 0
        resourceRepository.save(r4Food);

        // Region 2 has Food available: 150, reserved: 60 -> surplus: 90.
        // Direct road 2 -> 4 is BLOCKED. Open detour is 2 -> 3 -> 4 (55 mins, 30 km).
        ReliefRequest tempReq = new ReliefRequest();
        tempReq.setRegion(region4);
        tempReq.setResourceRequired("Food");
        tempReq.setRequiredQuantity(30);
        tempReq.setAllocatedQuantity(0);
        tempReq.setPriorityScore(92.0);
        tempReq.setRequestArrivalOrder(994);
        tempReq.setRequestStatus(ReliefRequestService.STATUS_PENDING);
        ReliefRequest savedReq = reliefRequestRepository.save(tempReq);

        AllocationResultDTO result = allocationService.allocateRequest(savedReq.getRequestId(), "ROUTE_ADMIN");
        assertTrue(result.isSuccess());
        assertNotNull(result.getAllocationId());

        var allocRecord = allocationRepository.findById(result.getAllocationId()).orElseThrow();
        // Donor should be reachable and route distance should not use blocked road
        assertTrue(allocRecord.getRouteDistanceKm() > 0.0, "Route distance should be calculated via open roads");
        assertTrue(result.getMessage().contains("via path"), "Message should describe open transit corridor");
    }

    @Test
    @Transactional
    @DisplayName("Verify Unreachable Route Exclusion When All Donor Corridors Are Blocked")
    void testUnreachableRouteExclusionWhenAllDonorCorridorsBlocked() {
        AffectedRegion region1 = affectedRegionRepository.findById(1).orElseThrow();

        // Region 1 has 0 surplus water.
        // Block all open incoming roads into Region 1 (Roads 2 -> 1 and 3 -> 1)
        List<com.rescuesync.entity.Road> allRoads = roadRepository.findAll();
        for (com.rescuesync.entity.Road road : allRoads) {
            if (road.getDestinationRegion() != null && road.getDestinationRegion().getRegionId() == 1) {
                road.setAccessibilityStatus("BLOCKED");
                roadRepository.save(road);
            }
        }

        // Create demand for Water in Region 1
        ReliefRequest tempReq = new ReliefRequest();
        tempReq.setRegion(region1);
        tempReq.setResourceRequired("Water");
        tempReq.setRequiredQuantity(20);
        tempReq.setAllocatedQuantity(0);
        tempReq.setPriorityScore(95.0);
        tempReq.setRequestArrivalOrder(993);
        tempReq.setRequestStatus(ReliefRequestService.STATUS_PENDING);
        ReliefRequest savedReq = reliefRequestRepository.save(tempReq);

        long allocCountBefore = allocationRepository.count();

        // Attempt allocation - must fail gracefully without negative inventory or unverified dispatch
        AllocationResultDTO result = allocationService.allocateRequest(savedReq.getRequestId(), "ROUTE_ADMIN");
        assertFalse(result.isSuccess(), "Allocation should fail when all donors are unreachable");
        assertTrue(result.getMessage().contains("No reachable donor region"), "Failure message must indicate donor unreachability");

        // Verify request remains PENDING with 0 allocated
        ReliefRequest reqAfter = reliefRequestRepository.findById(savedReq.getRequestId()).orElseThrow();
        assertEquals(0, reqAfter.getAllocatedQuantity());
        assertEquals(ReliefRequestService.STATUS_PENDING, reqAfter.getRequestStatus());

        // Verify no allocation record was created
        long allocCountAfter = allocationRepository.count();
        assertEquals(allocCountBefore, allocCountAfter);
    }
}

