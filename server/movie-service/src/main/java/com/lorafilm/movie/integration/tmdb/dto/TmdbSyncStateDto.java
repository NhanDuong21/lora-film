package com.lorafilm.movie.integration.tmdb.dto;

import java.time.LocalDate;
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
    private String scope;
    private LocalDate releaseDateFrom;
    private LocalDate releaseDateTo;
    private Integer maxMovies;
    private int processedMovies;
    private int importedMovies;
    private int skippedMovies;
    private String message;
    private boolean automaticSyncEnabled;

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
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public LocalDate getReleaseDateFrom() { return releaseDateFrom; }
    public void setReleaseDateFrom(LocalDate releaseDateFrom) { this.releaseDateFrom = releaseDateFrom; }
    public LocalDate getReleaseDateTo() { return releaseDateTo; }
    public void setReleaseDateTo(LocalDate releaseDateTo) { this.releaseDateTo = releaseDateTo; }
    public Integer getMaxMovies() { return maxMovies; }
    public void setMaxMovies(Integer maxMovies) { this.maxMovies = maxMovies; }
    public int getProcessedMovies() { return processedMovies; }
    public void setProcessedMovies(int processedMovies) { this.processedMovies = processedMovies; }
    public int getImportedMovies() { return importedMovies; }
    public void setImportedMovies(int importedMovies) { this.importedMovies = importedMovies; }
    public int getSkippedMovies() { return skippedMovies; }
    public void setSkippedMovies(int skippedMovies) { this.skippedMovies = skippedMovies; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isAutomaticSyncEnabled() { return automaticSyncEnabled; }
    public void setAutomaticSyncEnabled(boolean automaticSyncEnabled) { this.automaticSyncEnabled = automaticSyncEnabled; }
}
