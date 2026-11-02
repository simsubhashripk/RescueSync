package com.rescuesync.dto;

import java.util.List;

public class RouteResponse {

private Integer sourceRegionId;
private Integer destinationRegionId;

private List<Integer> regionPath;

private double totalTravelTime;
private double totalDistanceKm;

private boolean routeAvailable;
private String message;

public RouteResponse() {
}

public RouteResponse(
        Integer sourceRegionId,
        Integer destinationRegionId,
        List<Integer> regionPath,
        double totalTravelTime,
        double totalDistanceKm,
        boolean routeAvailable,
        String message) {

    this.sourceRegionId = sourceRegionId;
    this.destinationRegionId = destinationRegionId;
    this.regionPath = regionPath;
    this.totalTravelTime = totalTravelTime;
    this.totalDistanceKm = totalDistanceKm;
    this.routeAvailable = routeAvailable;
    this.message = message;
}

public Integer getSourceRegionId() {
    return sourceRegionId;
}

public void setSourceRegionId(Integer sourceRegionId) {
    this.sourceRegionId = sourceRegionId;
}

public Integer getDestinationRegionId() {
    return destinationRegionId;
}

public void setDestinationRegionId(Integer destinationRegionId) {
    this.destinationRegionId = destinationRegionId;
}

public List<Integer> getRegionPath() {
    return regionPath;
}

public void setRegionPath(List<Integer> regionPath) {
    this.regionPath = regionPath;
}

public double getTotalTravelTime() {
    return totalTravelTime;
}

public void setTotalTravelTime(double totalTravelTime) {
    this.totalTravelTime = totalTravelTime;
}

public double getTotalDistanceKm() {
    return totalDistanceKm;
}

public void setTotalDistanceKm(double totalDistanceKm) {
    this.totalDistanceKm = totalDistanceKm;
}

public boolean isRouteAvailable() {
    return routeAvailable;
}

public void setRouteAvailable(boolean routeAvailable) {
    this.routeAvailable = routeAvailable;
}

public String getMessage() {
    return message;
}

public void setMessage(String message) {
    this.message = message;
}


}
