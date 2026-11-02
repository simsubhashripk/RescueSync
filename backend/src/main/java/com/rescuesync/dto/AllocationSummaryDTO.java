package com.rescuesync.dto;

import java.util.Map;

public class AllocationSummaryDTO {
    private long totalAllocations;
    private long totalUnitsAllocated;
    private long pendingRequestsCount;
    private long partiallyAllocatedRequestsCount;
    private long fullyAllocatedRequestsCount;
    private long activeQueueCount;
    private Map<String, Long> resourceDistribution;

    public AllocationSummaryDTO() {
    }

    public AllocationSummaryDTO(long totalAllocations, long totalUnitsAllocated,
                                long pendingRequestsCount, long partiallyAllocatedRequestsCount,
                                long fullyAllocatedRequestsCount, long activeQueueCount,
                                Map<String, Long> resourceDistribution) {
        this.totalAllocations = totalAllocations;
        this.totalUnitsAllocated = totalUnitsAllocated;
        this.pendingRequestsCount = pendingRequestsCount;
        this.partiallyAllocatedRequestsCount = partiallyAllocatedRequestsCount;
        this.fullyAllocatedRequestsCount = fullyAllocatedRequestsCount;
        this.activeQueueCount = activeQueueCount;
        this.resourceDistribution = resourceDistribution;
    }

    public long getTotalAllocations() {
        return totalAllocations;
    }

    public void setTotalAllocations(long totalAllocations) {
        this.totalAllocations = totalAllocations;
    }

    public long getTotalUnitsAllocated() {
        return totalUnitsAllocated;
    }

    public void setTotalUnitsAllocated(long totalUnitsAllocated) {
        this.totalUnitsAllocated = totalUnitsAllocated;
    }

    public long getPendingRequestsCount() {
        return pendingRequestsCount;
    }

    public void setPendingRequestsCount(long pendingRequestsCount) {
        this.pendingRequestsCount = pendingRequestsCount;
    }

    public long getPartiallyAllocatedRequestsCount() {
        return partiallyAllocatedRequestsCount;
    }

    public void setPartiallyAllocatedRequestsCount(long partiallyAllocatedRequestsCount) {
        this.partiallyAllocatedRequestsCount = partiallyAllocatedRequestsCount;
    }

    public long getFullyAllocatedRequestsCount() {
        return fullyAllocatedRequestsCount;
    }

    public void setFullyAllocatedRequestsCount(long fullyAllocatedRequestsCount) {
        this.fullyAllocatedRequestsCount = fullyAllocatedRequestsCount;
    }

    public long getActiveQueueCount() {
        return activeQueueCount;
    }

    public void setActiveQueueCount(long activeQueueCount) {
        this.activeQueueCount = activeQueueCount;
    }

    public Map<String, Long> getResourceDistribution() {
        return resourceDistribution;
    }

    public void setResourceDistribution(Map<String, Long> resourceDistribution) {
        this.resourceDistribution = resourceDistribution;
    }
}

