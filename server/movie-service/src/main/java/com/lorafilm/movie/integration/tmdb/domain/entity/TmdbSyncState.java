package com.lorafilm.movie.integration.tmdb.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tmdb_sync_state")
public class TmdbSyncState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sync_type", nullable = false, unique = true)
    private String syncType;

    @Column(name = "`cursor`")
    private String cursor;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "last_sync_time")
    private LocalDateTime lastSyncTime;

    @Column(name = "sync_scope")
    private String syncScope;

    @Column(name = "release_date_from")
    private LocalDate releaseDateFrom;

    @Column(name = "release_date_to")
    private LocalDate releaseDateTo;

    @Column(name = "max_movies")
    private Integer maxMovies;

    @Column(name = "processed_movies", nullable = false)
    private Integer processedMovies = 0;

    @Column(name = "imported_movies", nullable = false)
    private Integer importedMovies = 0;

    @Column(name = "skipped_movies", nullable = false)
    private Integer skippedMovies = 0;

    @Column(name = "status_message", length = 500)
    private String statusMessage;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSyncType() { return syncType; }
    public void setSyncType(String syncType) { this.syncType = syncType; }
    public String getCursor() { return cursor; }
    public void setCursor(String cursor) { this.cursor = cursor; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getLastSyncTime() { return lastSyncTime; }
    public void setLastSyncTime(LocalDateTime lastSyncTime) { this.lastSyncTime = lastSyncTime; }
    public String getSyncScope() { return syncScope; }
    public void setSyncScope(String syncScope) { this.syncScope = syncScope; }
    public LocalDate getReleaseDateFrom() { return releaseDateFrom; }
    public void setReleaseDateFrom(LocalDate releaseDateFrom) { this.releaseDateFrom = releaseDateFrom; }
    public LocalDate getReleaseDateTo() { return releaseDateTo; }
    public void setReleaseDateTo(LocalDate releaseDateTo) { this.releaseDateTo = releaseDateTo; }
    public Integer getMaxMovies() { return maxMovies; }
    public void setMaxMovies(Integer maxMovies) { this.maxMovies = maxMovies; }
    public Integer getProcessedMovies() { return processedMovies; }
    public void setProcessedMovies(Integer processedMovies) { this.processedMovies = processedMovies; }
    public Integer getImportedMovies() { return importedMovies; }
    public void setImportedMovies(Integer importedMovies) { this.importedMovies = importedMovies; }
    public Integer getSkippedMovies() { return skippedMovies; }
    public void setSkippedMovies(Integer skippedMovies) { this.skippedMovies = skippedMovies; }
    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
