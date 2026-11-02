package com.rescuesync.dto;

import com.rescuesync.entity.ReliefRequest;
import java.time.LocalDateTime;

public class ReliefRequestResponseDTO {
private Integer requestId;
private Integer regionId;
private String regionName;
private String resourceRequired;
private Integer requiredQuantity;
private Integer allocatedQuantity;
private Integer remainingDeficit;
private Double priorityScore;
private Integer requestArrivalOrder;
private String requestStatus;
private LocalDateTime createdAt;
private Integer queueRank;

public ReliefRequestResponseDTO() {
}

public static ReliefRequestResponseDTO fromEntity(ReliefRequest entity) {
    return fromEntity(entity, null);
}

public static ReliefRequestResponseDTO fromEntity(ReliefRequest entity, Integer queueRank) {
    if (entity == null) {
        return null;
    }

    ReliefRequestResponseDTO dto = new ReliefRequestResponseDTO();

    dto.setRequestId(entity.getRequestId());

    if (entity.getRegion() != null) {
        dto.setRegionId(entity.getRegion().getRegionId());
        dto.setRegionName(entity.getRegion().getRegionName());
    }

    dto.setResourceRequired(entity.getResourceRequired());
    dto.setRequiredQuantity(entity.getRequiredQuantity());
    dto.setAllocatedQuantity(entity.getAllocatedQuantity());

    // Safely handle nullable quantities without unnecessary unboxing calls.
    Integer requiredQty = entity.getRequiredQuantity();
    Integer allocatedQty = entity.getAllocatedQuantity();

    int req = 0;
    int alloc = 0;

    if (requiredQty != null) {
        req = requiredQty;
    }

    if (allocatedQty != null) {
        alloc = allocatedQty;
    }

    dto.setRemainingDeficit(Math.max(0, req - alloc));

    dto.setPriorityScore(entity.getPriorityScore());
    dto.setRequestArrivalOrder(entity.getRequestArrivalOrder());
    dto.setRequestStatus(entity.getRequestStatus());
    dto.setCreatedAt(entity.getCreatedAt());
    dto.setQueueRank(queueRank);

    return dto;
}

// Getters and Setters

public Integer getRequestId() {
    return requestId;
}

public void setRequestId(Integer requestId) {
    this.requestId = requestId;
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

public String getResourceRequired() {
    return resourceRequired;
}

public void setResourceRequired(String resourceRequired) {
    this.resourceRequired = resourceRequired;
}

public Integer getRequiredQuantity() {
    return requiredQuantity;
}

public void setRequiredQuantity(Integer requiredQuantity) {
    this.requiredQuantity = requiredQuantity;
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

public Double getPriorityScore() {
    return priorityScore;
}

public void setPriorityScore(Double priorityScore) {
    this.priorityScore = priorityScore;
}

public Integer getRequestArrivalOrder() {
    return requestArrivalOrder;
}

public void setRequestArrivalOrder(Integer requestArrivalOrder) {
    this.requestArrivalOrder = requestArrivalOrder;
}

public String getRequestStatus() {
    return requestStatus;
}

public void setRequestStatus(String requestStatus) {
    this.requestStatus = requestStatus;
}

public LocalDateTime getCreatedAt() {
    return createdAt;
}

public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
}

public Integer getQueueRank() {
    return queueRank;
}

public void setQueueRank(Integer queueRank) {
    this.queueRank = queueRank;
}


}
