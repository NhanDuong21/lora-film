package com.lorafilm.movie.integration.tmdb.dto;

import java.time.LocalDateTime;

public class TmdbSyncStateDto {
    private String syncType;
    private String persistedStatus;
    private String displayStatus;
    private String cursor;
    private LocalDateTime lastSuccessfulSyncAt;
    private LocalDateTime stateUpdatedAt;
    private boolean stale;
    private int staleThresholdSeconds;

    public TmdbSyncStateDto() {}

    public String getSyncType() { return syncType; }
    public void setSyncType(String syncType) { this.syncType = syncType; }

    public String getPersistedStatus() { return persistedStatus; }
    public void setPersistedStatus(String persistedStatus) { this.persistedStatus = persistedStatus; }

    public String getDisplayStatus() { return displayStatus; }
    public void setDisplayStatus(String displayStatus) { this.displayStatus = displayStatus; }

    public String getCursor() { return cursor; }
    public void setCursor(String cursor) { this.cursor = cursor; }

    public LocalDateTime getLastSuccessfulSyncAt() { return lastSuccessfulSyncAt; }
    public void setLastSuccessfulSyncAt(LocalDateTime lastSuccessfulSyncAt) { this.lastSuccessfulSyncAt = lastSuccessfulSyncAt; }

    public LocalDateTime getStateUpdatedAt() { return stateUpdatedAt; }
    public void setStateUpdatedAt(LocalDateTime stateUpdatedAt) { this.stateUpdatedAt = stateUpdatedAt; }

    public boolean isStale() { return stale; }
    public void setStale(boolean stale) { this.stale = stale; }

    public int getStaleThresholdSeconds() { return staleThresholdSeconds; }
    public void setStaleThresholdSeconds(int staleThresholdSeconds) { this.staleThresholdSeconds = staleThresholdSeconds; }
}
