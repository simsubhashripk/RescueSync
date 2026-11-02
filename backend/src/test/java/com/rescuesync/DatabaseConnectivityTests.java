package com.rescuesync;

import com.rescuesync.entity.AffectedRegion;
import com.rescuesync.entity.ReliefRequest;
import com.rescuesync.entity.Resource;
import com.rescuesync.repository.AffectedRegionRepository;
import com.rescuesync.repository.ReliefRequestRepository;
import com.rescuesync.repository.ResourceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DatabaseConnectivityTests {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private AffectedRegionRepository regionRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private ReliefRequestRepository requestRepository;

    @Test
    @DisplayName("Verify MySQL connection to rescuesync_db")
    void testDatabaseConnection() throws SQLException {
        assertNotNull(dataSource, "DataSource should be configured");
        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection, "JDBC Connection should be established");
            assertFalse(connection.isClosed(), "JDBC Connection should be open");
            String catalog = connection.getCatalog();
            System.out.println("[TEST] Connected to database catalog: " + catalog);
            assertEquals("rescuesync_db", catalog, "Connected catalog should be rescuesync_db");
        }
    }

    @Test
    @DisplayName("Verify reading existing affected_regions records from database")
    void testReadAffectedRegions() {
        List<AffectedRegion> regions = regionRepository.findAll();
        assertNotNull(regions, "Regions list should not be null");
        assertFalse(regions.isEmpty(), "Database should contain existing affected_regions records");
        System.out.println("[TEST] Read " + regions.size() + " affected regions from rescuesync_db:");
        for (AffectedRegion region : regions) {
            System.out.printf("   - [%d] %s (Severity: %d, Priority: %.2f, Lat: %.4f, Lon: %.4f)%n",
                    region.getRegionId(), region.getRegionName(), region.getDisasterSeverity(),
                    region.getPriorityScore(), region.getLatitude(), region.getLongitude());
        }
    }

    @Test
    @DisplayName("Verify reading existing resources records from database")
    void testReadResources() {
        List<Resource> resources = resourceRepository.findAll();
        assertNotNull(resources, "Resources list should not be null");
        assertFalse(resources.isEmpty(), "Database should contain existing resources records");
        System.out.println("[TEST] Read " + resources.size() + " resources from rescuesync_db:");
        for (Resource res : resources) {
            System.out.printf("   - [%d] Region #%d: %s (Available: %d, Reserved: %d)%n",
                    res.getResourceId(), res.getRegion().getRegionId(), res.getResourceName(),
                    res.getAvailableQuantity(), res.getReservedQuantity());
        }
    }

    @Test
    @DisplayName("Verify reading existing relief_requests records from database")
    void testReadReliefRequests() {
        List<ReliefRequest> requests = requestRepository.findAll();
        assertNotNull(requests, "Relief requests list should not be null");
        assertFalse(requests.isEmpty(), "Database should contain existing relief_requests records");
        System.out.println("[TEST] Read " + requests.size() + " relief requests from rescuesync_db:");
        for (ReliefRequest req : requests) {
            System.out.printf("   - Req #%d: %s (%d needed, %d allocated) for Region '%s' [Status: %s]%n",
                    req.getRequestId(), req.getResourceRequired(), req.getRequiredQuantity(),
                    req.getAllocatedQuantity(), req.getRegion().getRegionName(), req.getRequestStatus());
        }
    }
}

