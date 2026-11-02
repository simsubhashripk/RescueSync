package com.rescuesync.dto;

import java.util.List;

public class BatchAllocationResultDTO {
    private int processedCount;
    private int successfulAllocations;
    private int unfulfilledRequests;
    private int totalUnitsAllocated;
    private List<AllocationResultDTO> details;

    public BatchAllocationResultDTO() {
    }

    public BatchAllocationResultDTO(int processedCount, int successfulAllocations,
                                    int unfulfilledRequests, int totalUnitsAllocated,
                                    List<AllocationResultDTO> details) {
        this.processedCount = processedCount;
        this.successfulAllocations = successfulAllocations;
        this.unfulfilledRequests = unfulfilledRequests;
        this.totalUnitsAllocated = totalUnitsAllocated;
        this.details = details;
    }

    public int getProcessedCount() {
        return processedCount;
    }

    public void setProcessedCount(int processedCount) {
        this.processedCount = processedCount;
    }

    public int getSuccessfulAllocations() {
        return successfulAllocations;
    }

    public void setSuccessfulAllocations(int successfulAllocations) {
        this.successfulAllocations = successfulAllocations;
    }

    public int getUnfulfilledRequests() {
        return unfulfilledRequests;
    }

    public void setUnfulfilledRequests(int unfulfilledRequests) {
        this.unfulfilledRequests = unfulfilledRequests;
    }

    public int getTotalUnitsAllocated() {
        return totalUnitsAllocated;
    }

    public void setTotalUnitsAllocated(int totalUnitsAllocated) {
        this.totalUnitsAllocated = totalUnitsAllocated;
    }

    public List<AllocationResultDTO> getDetails() {
        return details;
    }

    public void setDetails(List<AllocationResultDTO> details) {
        this.details = details;
    }
}

