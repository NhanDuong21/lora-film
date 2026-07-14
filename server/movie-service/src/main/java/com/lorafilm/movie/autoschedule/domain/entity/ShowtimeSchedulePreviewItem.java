package com.lorafilm.movie.autoschedule.domain.entity;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemApplyStatus;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "showtime_schedule_preview_items", uniqueConstraints = {
    @UniqueConstraint(name = "uk_preview_items_preview_auditorium_start", columnNames = {"preview_id", "auditorium_id", "start_time"})
})
public class ShowtimeSchedulePreviewItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preview_id", nullable = false)
    private ShowtimeSchedulePreview preview;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_version_id", nullable = false)
    private MovieVersion movieVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_id", nullable = false)
    private Cinema cinema;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auditorium_id", nullable = false)
    private Auditorium auditorium;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "occupancy_end_time", nullable = false)
    private Instant occupancyEndTime;

    @Column(name = "score", nullable = false)
    private Double score;

    @Column(name = "score_breakdown_json", columnDefinition = "json")
    private String scoreBreakdownJson;

    @Column(name = "ranking_position", nullable = false)
    private Integer rankingPosition;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", nullable = false)
    private PreviewItemValidationStatus validationStatus;

    @Column(name = "rejection_code")
    private String rejectionCode;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "selected", nullable = false)
    private Boolean selected;

    @Column(name = "selected_at")
    private Instant selectedAt;

    @Column(name = "selected_by")
    private Long selectedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "apply_status", nullable = false)
    private PreviewItemApplyStatus applyStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_showtime_id")
    private Showtime createdShowtime;

    @Column(name = "apply_error_code")
    private String applyErrorCode;

    @Column(name = "apply_error_message")
    private String applyErrorMessage;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Instant updatedAt;

    public ShowtimeSchedulePreviewItem() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public ShowtimeSchedulePreview getPreview() {
        return preview;
    }

    public void setPreview(ShowtimeSchedulePreview preview) {
        this.preview = preview;
    }

    public Movie getMovie() {
        return movie;
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
    }

    public MovieVersion getMovieVersion() {
        return movieVersion;
    }

    public void setMovieVersion(MovieVersion movieVersion) {
        this.movieVersion = movieVersion;
    }

    public Cinema getCinema() {
        return cinema;
    }

    public void setCinema(Cinema cinema) {
        this.cinema = cinema;
    }

    public Auditorium getAuditorium() {
        return auditorium;
    }

    public void setAuditorium(Auditorium auditorium) {
        this.auditorium = auditorium;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public Instant getOccupancyEndTime() {
        return occupancyEndTime;
    }

    public void setOccupancyEndTime(Instant occupancyEndTime) {
        this.occupancyEndTime = occupancyEndTime;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getScoreBreakdownJson() {
        return scoreBreakdownJson;
    }

    public void setScoreBreakdownJson(String scoreBreakdownJson) {
        this.scoreBreakdownJson = scoreBreakdownJson;
    }

    public Integer getRankingPosition() {
        return rankingPosition;
    }

    public void setRankingPosition(Integer rankingPosition) {
        this.rankingPosition = rankingPosition;
    }

    public PreviewItemValidationStatus getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(PreviewItemValidationStatus validationStatus) {
        this.validationStatus = validationStatus;
    }

    public String getRejectionCode() {
        return rejectionCode;
    }

    public void setRejectionCode(String rejectionCode) {
        this.rejectionCode = rejectionCode;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Boolean getSelected() {
        return selected;
    }

    public void setSelected(Boolean selected) {
        this.selected = selected;
    }

    public Instant getSelectedAt() {
        return selectedAt;
    }

    public void setSelectedAt(Instant selectedAt) {
        this.selectedAt = selectedAt;
    }

    public Long getSelectedBy() {
        return selectedBy;
    }

    public void setSelectedBy(Long selectedBy) {
        this.selectedBy = selectedBy;
    }

    public PreviewItemApplyStatus getApplyStatus() {
        return applyStatus;
    }

    public void setApplyStatus(PreviewItemApplyStatus applyStatus) {
        this.applyStatus = applyStatus;
    }

    public Showtime getCreatedShowtime() {
        return createdShowtime;
    }

    public void setCreatedShowtime(Showtime createdShowtime) {
        this.createdShowtime = createdShowtime;
    }

    public String getApplyErrorCode() {
        return applyErrorCode;
    }

    public void setApplyErrorCode(String applyErrorCode) {
        this.applyErrorCode = applyErrorCode;
    }

    public String getApplyErrorMessage() {
        return applyErrorMessage;
    }

    public void setApplyErrorMessage(String applyErrorMessage) {
        this.applyErrorMessage = applyErrorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
