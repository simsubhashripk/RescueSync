package com.rescuesync.dto;

public class ResourceAuditReportDTO {
    private Integer regionId;
    private String regionName;
    private String resourceName;
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private Integer usableStock;
    private Integer pendingDeficit;
    private Integer shortageQuantity;
    private Integer surplusQuantity;
    private Double priorityScore;

    public ResourceAuditReportDTO() {
    }

    public ResourceAuditReportDTO(Integer regionId, String regionName, String resourceName,
                                  Integer availableQuantity, Integer reservedQuantity,
                                  Integer usableStock, Integer pendingDeficit,
                                  Integer shortageQuantity, Integer surplusQuantity,
                                  Double priorityScore) {
        this.regionId = regionId;
        this.regionName = regionName;
        this.resourceName = resourceName;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = reservedQuantity;
        this.usableStock = usableStock;
        this.pendingDeficit = pendingDeficit;
        this.shortageQuantity = shortageQuantity;
        this.surplusQuantity = surplusQuantity;
        this.priorityScore = priorityScore;
    }

    public Integer getRegionId() {
        return regionId;
    }

    public void setRegionId(Integer regionId) {
        this.regionId = regionId;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public Integer getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public Integer getUsableStock() {
        return usableStock;
    }

    public void setUsableStock(Integer usableStock) {
        this.usableStock = usableStock;
    }

    public Integer getPendingDeficit() {
        return pendingDeficit;
    }

    public void setPendingDeficit(Integer pendingDeficit) {
        this.pendingDeficit = pendingDeficit;
    }

    public Integer getShortageQuantity() {
        return shortageQuantity;
    }

    public void setShortageQuantity(Integer shortageQuantity) {
        this.shortageQuantity = shortageQuantity;
    }

    public Integer getSurplusQuantity() {
        return surplusQuantity;
    }

    public void setSurplusQuantity(Integer surplusQuantity) {
        this.surplusQuantity = surplusQuantity;
    }

    public Double getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(Double priorityScore) {
        this.priorityScore = priorityScore;
    }
}

