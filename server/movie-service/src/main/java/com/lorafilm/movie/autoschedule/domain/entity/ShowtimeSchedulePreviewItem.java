package com.lorafilm.movie.autoschedule.domain.entity;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemApplyStatus;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import jakarta.persistence.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "showtime_schedule_preview_items", uniqueConstraints = {
    @UniqueConstraint(name = "uk_schedule_preview_item_slot", columnNames = {"preview_id", "auditorium_id", "start_time"})
})
@org.hibernate.annotations.Check(constraints = "(validation_status != 'REJECTED' OR selected = false)")
public class ShowtimeSchedulePreviewItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 36, columnDefinition = "CHAR(36)")
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

    @Column(name = "score", nullable = false, precision = 10, scale = 3)
    private BigDecimal score;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "score_breakdown_json", columnDefinition = "json")
    private Map<String, BigDecimal> scoreBreakdown;

    @Column(name = "ranking_position", nullable = false)
    private Integer rankingPosition;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", nullable = false, length = 30)
    private PreviewItemValidationStatus validationStatus;

    @Column(name = "rejection_code", length = 100)
    private String rejectionCode;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "selected", nullable = false)
    private Boolean selected;

    @Column(name = "selected_at")
    private Instant selectedAt;

    @Column(name = "selected_by")
    private Long selectedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "apply_status", nullable = false, length = 30)
    private PreviewItemApplyStatus applyStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_showtime_id")
    private Showtime createdShowtime;

    @Column(name = "apply_error_code", length = 100)
    private String applyErrorCode;

    @Column(name = "apply_error_message", length = 500)
    private String applyErrorMessage;

    @org.hibernate.annotations.CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @org.hibernate.annotations.UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    protected ShowtimeSchedulePreviewItem() {}

    public static ShowtimeSchedulePreviewItem createItem(ShowtimeSchedulePreview preview, com.lorafilm.movie.autoschedule.model.ShowtimeCandidate candidate) {
        ShowtimeSchedulePreviewItem item = new ShowtimeSchedulePreviewItem();
        item.setPublicId(java.util.UUID.randomUUID().toString());
        item.setPreview(preview);
        
        item.setMovie(candidate.getMovie());
        item.setMovieVersion(candidate.getMovieVersion());
        item.setCinema(candidate.getCinema());
        item.setAuditorium(candidate.getAuditorium());
        
        item.setStartTime(candidate.getStartTime());
        item.setEndTime(candidate.getEndTime());
        item.setOccupancyEndTime(candidate.getOccupancyEndTime());
        
        item.setScore(candidate.getScore());
        item.setScoreBreakdown(candidate.getScoreBreakdown());
        item.setRankingPosition(candidate.getRankingPosition());
        item.setValidationStatus(candidate.getValidationStatus());
        item.setRejectionCode(candidate.getRejectionCode());
        item.setRejectionReason(candidate.getRejectionReason());
        
        item.setSelected(candidate.isSelected());
        item.setSelectedAt(null);
        item.setSelectedBy(null);
        item.setApplyStatus(PreviewItemApplyStatus.PENDING);
        return item;
    }

    public Long getId() {
        return id;
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

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public Map<String, BigDecimal> getScoreBreakdown() {
        return scoreBreakdown;
    }

    public void setScoreBreakdown(Map<String, BigDecimal> scoreBreakdown) {
        this.scoreBreakdown = scoreBreakdown;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
