package com.rescuesync.dto;

public class AllocationResultDTO {
    private boolean success;
    private String message;
    private Integer requestId;
    private Integer allocatedQuantity;
    private Integer remainingDeficit;
    private String requestStatus;
    private Integer supplyRegionId;
    private String supplyRegionName;
    private Integer allocationId;

    public AllocationResultDTO() {
    }

    public AllocationResultDTO(boolean success, String message, Integer requestId,
                               Integer allocatedQuantity, Integer remainingDeficit,
                               String requestStatus, Integer supplyRegionId,
                               String supplyRegionName, Integer allocationId) {
        this.success = success;
        this.message = message;
        this.requestId = requestId;
        this.allocatedQuantity = allocatedQuantity;
        this.remainingDeficit = remainingDeficit;
        this.requestStatus = requestStatus;
        this.supplyRegionId = supplyRegionId;
        this.supplyRegionName = supplyRegionName;
        this.allocationId = allocationId;
    }

    public static AllocationResultDTO failure(Integer requestId, String message) {
        return new AllocationResultDTO(false, message, requestId, 0, null, null, null, null, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getRequestId() {
        return requestId;
    }

    public void setRequestId(Integer requestId) {
        this.requestId = requestId;
    }

    public Integer getAllocatedQuantity() {
        return allocatedQuantity;
    }

    public void setAllocatedQuantity(Integer allocatedQuantity) {
        this.allocatedQuantity = allocatedQuantity;
    }

    public Integer getRemainingDeficit() {
        return remainingDeficit;
    }

    public void setRemainingDeficit(Integer remainingDeficit) {
        this.remainingDeficit = remainingDeficit;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
    }

    public Integer getSupplyRegionId() {
        return supplyRegionId;
    }

    public void setSupplyRegionId(Integer supplyRegionId) {
        this.supplyRegionId = supplyRegionId;
    }

    public String getSupplyRegionName() {
        return supplyRegionName;
    }

    public void setSupplyRegionName(String supplyRegionName) {
        this.supplyRegionName = supplyRegionName;
    }

    public Integer getAllocationId() {
        return allocationId;
    }

    public void setAllocationId(Integer allocationId) {
        this.allocationId = allocationId;
    }
}

