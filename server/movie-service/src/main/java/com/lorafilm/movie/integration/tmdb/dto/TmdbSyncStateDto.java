package com.lorafilm.movie.integration.tmdb.dto;

import java.time.LocalDateTime;

public class TmdbSyncStateDto {
    private String status;
    private String cursor;
    private LocalDateTime startedAt;
    private LocalDateTime lastCompletedAt;
    private LocalDateTime lastSuccessAt;
    private LocalDateTime lastFailureAt;
    private String lastError;

    public TmdbSyncStateDto() {}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCursor() { return cursor; }
    public void setCursor(String cursor) { this.cursor = cursor; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getLastCompletedAt() { return lastCompletedAt; }
    public void setLastCompletedAt(LocalDateTime lastCompletedAt) { this.lastCompletedAt = lastCompletedAt; }

    public LocalDateTime getLastSuccessAt() { return lastSuccessAt; }
    public void setLastSuccessAt(LocalDateTime lastSuccessAt) { this.lastSuccessAt = lastSuccessAt; }

    public LocalDateTime getLastFailureAt() { return lastFailureAt; }
    public void setLastFailureAt(LocalDateTime lastFailureAt) { this.lastFailureAt = lastFailureAt; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
}
