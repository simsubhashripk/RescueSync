package com.rescuesync.service;

import com.rescuesync.dto.RouteResponse;
import com.rescuesync.entity.Road;
import com.rescuesync.repository.AffectedRegionRepository;
import com.rescuesync.repository.RoadRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class RouteService {

    private final RoadRepository roadRepository;
    private final AffectedRegionRepository regionRepository;

    public RouteService(
            RoadRepository roadRepository,
            AffectedRegionRepository regionRepository) {

        this.roadRepository = roadRepository;
        this.regionRepository = regionRepository;
    }

    /**
     * Finds the fastest available route using Dijkstra's algorithm.
     * Blocked roads are excluded from route calculation.
     */
    @Transactional(readOnly = true)
    public RouteResponse findShortestRoute(
            Integer sourceRegionId,
            Integer destinationRegionId) {

        // Validate input
        if (sourceRegionId == null || destinationRegionId == null) {
            throw new IllegalArgumentException(
                    "Source and destination region IDs are required.");
        }

        // Check whether source region exists
        if (!regionRepository.existsById(sourceRegionId)) {
            throw new IllegalArgumentException(
                    "Source region not found: " + sourceRegionId);
        }

        // Check whether destination region exists
        if (!regionRepository.existsById(destinationRegionId)) {
            throw new IllegalArgumentException(
                    "Destination region not found: " + destinationRegionId);
        }

        // Same source and destination
        if (sourceRegionId.equals(destinationRegionId)) {
            return new RouteResponse(
                    sourceRegionId,
                    destinationRegionId,
                    List.of(sourceRegionId),
                    0.0,
                    0.0,
                    true,
                    "Source and destination are the same region."
            );
        }

        // Retrieve open roads
        List<Road> openRoads =
                roadRepository.findByAccessibilityStatus("OPEN");

        // Build adjacency list
        Map<Integer, List<Road>> graph = new HashMap<>();

        for (Road road : openRoads) {

            if (road.getSourceRegion() == null ||
                    road.getDestinationRegion() == null) {
                continue;
            }

            double travelTime = road.getTravelTime();
            double distance = road.getDistanceKm();

            // Ignore invalid road values
            if (!Double.isFinite(travelTime) ||
                    travelTime < 0 ||
                    !Double.isFinite(distance) ||
                    distance < 0) {
                continue;
            }

            Integer sourceId =
                    road.getSourceRegion().getRegionId();

            graph.computeIfAbsent(
                    sourceId,
                    key -> new ArrayList<>()
            ).add(road);
        }

        // Dijkstra data structures
        Map<Integer, Double> shortestTimes = new HashMap<>();

        Map<Integer, Integer> previousRegion = new HashMap<>();

        Map<Integer, Road> previousRoad = new HashMap<>();

        PriorityQueue<RouteNode> queue = new PriorityQueue<>(
                Comparator.comparingDouble(RouteNode::travelTime)
        );

        shortestTimes.put(sourceRegionId, 0.0);

        queue.offer(new RouteNode(sourceRegionId, 0.0));

        // Dijkstra algorithm
        while (!queue.isEmpty()) {

            RouteNode current = queue.poll();

            // Skip outdated queue entries
            if (current.travelTime() >
                    shortestTimes.getOrDefault(
                            current.regionId(),
                            Double.MAX_VALUE)) {
                continue;
            }

            // Destination reached
            if (current.regionId().equals(destinationRegionId)) {
                break;
            }

            List<Road> outgoingRoads =
                    graph.getOrDefault(
                            current.regionId(),
                            Collections.emptyList()
                    );

            for (Road road : outgoingRoads) {

                Integer nextRegionId =
                        road.getDestinationRegion().getRegionId();

                double newTime =
                        current.travelTime() + road.getTravelTime();

                // Relaxation
                if (newTime < shortestTimes.getOrDefault(
                        nextRegionId,
                        Double.MAX_VALUE)) {

                    shortestTimes.put(nextRegionId, newTime);

                    previousRegion.put(
                            nextRegionId,
                            current.regionId()
                    );

                    previousRoad.put(nextRegionId, road);

                    queue.offer(
                            new RouteNode(nextRegionId, newTime)
                    );
                }
            }
        }

        // Check route availability
        if (!shortestTimes.containsKey(destinationRegionId)) {

            return new RouteResponse(
                    sourceRegionId,
                    destinationRegionId,
                    Collections.emptyList(),
                    0.0,
                    0.0,
                    false,
                    "No open route is available between these regions."
            );
        }

        // Reconstruct shortest path
        List<Integer> path = new ArrayList<>();

        double totalDistance = 0.0;

        Integer currentRegionId = destinationRegionId;

        while (currentRegionId != null) {

            path.add(currentRegionId);

            if (currentRegionId.equals(sourceRegionId)) {
                break;
            }

            Road selectedRoad =
                    previousRoad.get(currentRegionId);

            if (selectedRoad == null) {
                throw new IllegalStateException(
                        "Unable to reconstruct the calculated route.");
            }

            totalDistance += selectedRoad.getDistanceKm();

            currentRegionId =
                    previousRegion.get(currentRegionId);
        }

        // Reverse path to get source -> destination
        Collections.reverse(path);

        // Return result
        return new RouteResponse(
                sourceRegionId,
                destinationRegionId,
                path,
                roundToTwoDecimals(
                        shortestTimes.get(destinationRegionId)),
                roundToTwoDecimals(totalDistance),
                true,
                "Fastest available route calculated successfully."
        );
    }

    /**
     * Rounds values to two decimal places.
     */
    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /**
     * Represents a node in the priority queue.
     */
    private record RouteNode(
            Integer regionId,
            double travelTime) {
    }
}