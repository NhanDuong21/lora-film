package com.lorafilm.movie.showtime.dto.response;

import java.time.Instant;

public class ShowtimeStatusHistoryResponse {

    private String previousStatus;
    private String newStatus;
    private String reason;
    private Instant changedAt;
    private Long changedBy;
    private String source;
    private String previewPublicId;

    public ShowtimeStatusHistoryResponse() {}

    public String getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Instant changedAt) {
        this.changedAt = changedAt;
    }

    public Long getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(Long changedBy) {
        this.changedBy = changedBy;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getPreviewPublicId() {
        return previewPublicId;
    }

    public void setPreviewPublicId(String previewPublicId) {
        this.previewPublicId = previewPublicId;
    }
}
