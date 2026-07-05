package com.project.scoreservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ScoreAdjustmentRequest {
    @NotNull(message = "Adjustment type is required")
    private ScoreAdjustmentType adjustmentType;

    @NotNull(message = "Points is required")
    @Min(value = 1, message = "Points must be greater than zero")
    private Integer points;

    private Boolean affectAccumulatedPoints = false;

    @NotBlank(message = "Reason is required")
    @Size(max = 255, message = "Reason cannot exceed 255 characters")
    private String reason;

    @NotBlank(message = "Request ID is required")
    @Size(max = 100, message = "Request ID cannot exceed 100 characters")
    private String requestId;

    public ScoreAdjustmentRequest() {
    }

    public ScoreAdjustmentRequest(ScoreAdjustmentType adjustmentType, Integer points, Boolean affectAccumulatedPoints, String reason, String requestId) {
        this.adjustmentType = adjustmentType;
        this.points = points;
        this.affectAccumulatedPoints = affectAccumulatedPoints != null ? affectAccumulatedPoints : false;
        this.reason = reason;
        this.requestId = requestId;
    }

    public ScoreAdjustmentType getAdjustmentType() {
        return adjustmentType;
    }

    public void setAdjustmentType(ScoreAdjustmentType adjustmentType) {
        this.adjustmentType = adjustmentType;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public Boolean getAffectAccumulatedPoints() {
        return affectAccumulatedPoints != null ? affectAccumulatedPoints : false;
    }

    public void setAffectAccumulatedPoints(Boolean affectAccumulatedPoints) {
        this.affectAccumulatedPoints = affectAccumulatedPoints != null ? affectAccumulatedPoints : false;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
