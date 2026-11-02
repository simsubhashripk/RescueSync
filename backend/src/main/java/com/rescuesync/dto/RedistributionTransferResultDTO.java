package com.rescuesync.dto;

public class RedistributionTransferResultDTO {
    private boolean success;
    private String message;
    private Integer recordId;
    private String status;
    private String sourceRegionName;
    private String destRegionName;
    private String resourceName;
    private Integer quantity;

    public RedistributionTransferResultDTO() {
    }

    public RedistributionTransferResultDTO(boolean success, String message, Integer recordId,
                                           String status, String sourceRegionName,
                                           String destRegionName, String resourceName,
                                           Integer quantity) {
        this.success = success;
        this.message = message;
        this.recordId = recordId;
        this.status = status;
        this.sourceRegionName = sourceRegionName;
        this.destRegionName = destRegionName;
        this.resourceName = resourceName;
        this.quantity = quantity;
    }

    public static RedistributionTransferResultDTO failure(Integer recordId, String message) {
        return new RedistributionTransferResultDTO(false, message, recordId, "FAILED", null, null, null, null);
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

    public Integer getRecordId() {
        return recordId;
    }

    public void setRecordId(Integer recordId) {
        this.recordId = recordId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSourceRegionName() {
        return sourceRegionName;
    }

    public void setSourceRegionName(String sourceRegionName) {
        this.sourceRegionName = sourceRegionName;
    }

    public String getDestRegionName() {
        return destRegionName;
    }

    public void setDestRegionName(String destRegionName) {
        this.destRegionName = destRegionName;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}

