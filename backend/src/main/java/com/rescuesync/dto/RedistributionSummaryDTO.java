package com.rescuesync.dto;

public class RedistributionSummaryDTO {
    private int totalShortages;
    private int totalSurpluses;
    private int recommendedTransfersCount;
    private int completedTransfersCount;
    private int totalUnitsRecommended;
    private int totalUnitsTransferred;

    public RedistributionSummaryDTO() {
    }

    public RedistributionSummaryDTO(int totalShortages, int totalSurpluses,
                                    int recommendedTransfersCount, int completedTransfersCount,
                                    int totalUnitsRecommended, int totalUnitsTransferred) {
        this.totalShortages = totalShortages;
        this.totalSurpluses = totalSurpluses;
        this.recommendedTransfersCount = recommendedTransfersCount;
        this.completedTransfersCount = completedTransfersCount;
        this.totalUnitsRecommended = totalUnitsRecommended;
        this.totalUnitsTransferred = totalUnitsTransferred;
    }

    public int getTotalShortages() {
        return totalShortages;
    }

    public void setTotalShortages(int totalShortages) {
        this.totalShortages = totalShortages;
    }

    public int getTotalSurpluses() {
        return totalSurpluses;
    }

    public void setTotalSurpluses(int totalSurpluses) {
        this.totalSurpluses = totalSurpluses;
    }

    public int getRecommendedTransfersCount() {
        return recommendedTransfersCount;
    }

    public void setRecommendedTransfersCount(int recommendedTransfersCount) {
        this.recommendedTransfersCount = recommendedTransfersCount;
    }

    public int getCompletedTransfersCount() {
        return completedTransfersCount;
    }

    public void setCompletedTransfersCount(int completedTransfersCount) {
        this.completedTransfersCount = completedTransfersCount;
    }

    public int getTotalUnitsRecommended() {
        return totalUnitsRecommended;
    }

    public void setTotalUnitsRecommended(int totalUnitsRecommended) {
        this.totalUnitsRecommended = totalUnitsRecommended;
    }

    public int getTotalUnitsTransferred() {
        return totalUnitsTransferred;
    }

    public void setTotalUnitsTransferred(int totalUnitsTransferred) {
        this.totalUnitsTransferred = totalUnitsTransferred;
    }
}

