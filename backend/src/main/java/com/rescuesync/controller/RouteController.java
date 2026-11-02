package com.rescuesync.controller;

import com.rescuesync.dto.RouteResponse;
import com.rescuesync.service.RouteService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    /**
     * Find the fastest available route between two disaster regions.
     */
    @GetMapping("/shortest")
    public ResponseEntity<RouteResponse> findShortestRoute(
            @RequestParam Integer sourceRegionId,
            @RequestParam Integer destinationRegionId) {

        RouteResponse response = routeService.findShortestRoute(
                sourceRegionId,
                destinationRegionId
        );

        return ResponseEntity.ok(response);
    }
}