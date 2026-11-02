package com.rescuesync.dto;

import java.util.List;

public class QueueSummaryDTO {

    private int totalRequests;
    private int activeQueueDemands;
    private int pendingCount;
    private int partiallyAllocatedCount;
    private int allocatedCount;
    private int totalRequiredUnits;
    private int totalAllocatedUnits;
    private int totalRemainingDeficit;
    private ReliefRequestResponseDTO topPriorityRequest;
    private List<ReliefRequestResponseDTO> activeQueue;

    public QueueSummaryDTO() {
    }

    public int getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(int totalRequests) {
        this.totalRequests = totalRequests;
    }

    public int getActiveQueueDemands() {
        return activeQueueDemands;
    }

    public void setActiveQueueDemands(int activeQueueDemands) {
        this.activeQueueDemands = activeQueueDemands;
    }

    public int getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(int pendingCount) {
        this.pendingCount = pendingCount;
    }

    public int getPartiallyAllocatedCount() {
        return partiallyAllocatedCount;
    }

    public void setPartiallyAllocatedCount(int partiallyAllocatedCount) {
        this.partiallyAllocatedCount = partiallyAllocatedCount;
    }

    public int getAllocatedCount() {
        return allocatedCount;
    }

    public void setAllocatedCount(int allocatedCount) {
        this.allocatedCount = allocatedCount;
    }

    public int getTotalRequiredUnits() {
        return totalRequiredUnits;
    }

    public void setTotalRequiredUnits(int totalRequiredUnits) {
        this.totalRequiredUnits = totalRequiredUnits;
    }

    public int getTotalAllocatedUnits() {
        return totalAllocatedUnits;
    }

    public void setTotalAllocatedUnits(int totalAllocatedUnits) {
        this.totalAllocatedUnits = totalAllocatedUnits;
    }

    public int getTotalRemainingDeficit() {
        return totalRemainingDeficit;
    }

    public void setTotalRemainingDeficit(int totalRemainingDeficit) {
        this.totalRemainingDeficit = totalRemainingDeficit;
    }

    public ReliefRequestResponseDTO getTopPriorityRequest() {
        return topPriorityRequest;
    }

    public void setTopPriorityRequest(ReliefRequestResponseDTO topPriorityRequest) {
        this.topPriorityRequest = topPriorityRequest;
    }

    public List<ReliefRequestResponseDTO> getActiveQueue() {
        return activeQueue;
    }

    public void setActiveQueue(List<ReliefRequestResponseDTO> activeQueue) {
        this.activeQueue = activeQueue;
    }
}

