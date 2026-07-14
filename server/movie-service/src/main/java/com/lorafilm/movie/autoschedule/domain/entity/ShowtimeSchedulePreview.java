package com.lorafilm.movie.autoschedule.domain.entity;

import com.lorafilm.movie.autoschedule.domain.enums.AutoScheduleStrategy;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewApplyMode;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "showtime_schedule_previews")
public class ShowtimeSchedulePreview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_id", nullable = false)
    private Cinema cinema;

    @Column(name = "schedule_from", nullable = false)
    private LocalDate scheduleFrom;

    @Column(name = "schedule_to", nullable = false)
    private LocalDate scheduleTo;

    @Column(name = "timezone_snapshot", nullable = false)
    private String timezoneSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "strategy", nullable = false)
    private AutoScheduleStrategy strategy;

    @Column(name = "strategy_version", nullable = false)
    private String strategyVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "apply_mode", nullable = false)
    private SchedulePreviewApplyMode applyMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SchedulePreviewStatus status;

    @Column(name = "slot_granularity_minutes", nullable = false)
    private Integer slotGranularityMinutes;

    @Column(name = "total_candidate_count", nullable = false)
    private Integer totalCandidateCount;

    @Column(name = "valid_candidate_count", nullable = false)
    private Integer validCandidateCount;

    @Column(name = "rejected_candidate_count", nullable = false)
    private Integer rejectedCandidateCount;

    @Column(name = "selected_candidate_count", nullable = false)
    private Integer selectedCandidateCount;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "generated_by", nullable = false)
    private Long generatedBy;

    @Column(name = "applied_at")
    private Instant appliedAt;

    @Column(name = "applied_by")
    private Long appliedBy;

    @Column(name = "generate_idempotency_key", nullable = false, unique = true)
    private String generateIdempotencyKey;

    @Column(name = "apply_idempotency_key", unique = true)
    private String applyIdempotencyKey;

    @Column(name = "request_fingerprint", nullable = false)
    private String requestFingerprint;

    @Column(name = "failure_reason")
    private String failureReason;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Instant updatedAt;

    @OneToMany(mappedBy = "preview", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShowtimeSchedulePreviewItem> items = new ArrayList<>();

    public ShowtimeSchedulePreview() {}

    public void addItem(ShowtimeSchedulePreviewItem item) {
        items.add(item);
        item.setPreview(this);
    }

    public void removeItem(ShowtimeSchedulePreviewItem item) {
        items.remove(item);
        item.setPreview(null);
    }
    
    // Domain behaviors
    public void markPreviewed() {
        this.status = SchedulePreviewStatus.PREVIEWED;
    }

    public void markApplying(String applyIdempotencyKey) {
        this.status = SchedulePreviewStatus.APPLYING;
        this.applyIdempotencyKey = applyIdempotencyKey;
    }

    public void markApplied(Long actorId, Instant appliedAt) {
        this.status = SchedulePreviewStatus.APPLIED;
        this.appliedBy = actorId;
        this.appliedAt = appliedAt;
    }

    public void markExpired() {
        this.status = SchedulePreviewStatus.EXPIRED;
    }

    public void markFailed(String reason) {
        this.status = SchedulePreviewStatus.FAILED;
        this.failureReason = reason;
    }

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

    public Cinema getCinema() {
        return cinema;
    }

    public void setCinema(Cinema cinema) {
        this.cinema = cinema;
    }

    public LocalDate getScheduleFrom() {
        return scheduleFrom;
    }

    public void setScheduleFrom(LocalDate scheduleFrom) {
        this.scheduleFrom = scheduleFrom;
    }

    public LocalDate getScheduleTo() {
        return scheduleTo;
    }

    public void setScheduleTo(LocalDate scheduleTo) {
        this.scheduleTo = scheduleTo;
    }

    public String getTimezoneSnapshot() {
        return timezoneSnapshot;
    }

    public void setTimezoneSnapshot(String timezoneSnapshot) {
        this.timezoneSnapshot = timezoneSnapshot;
    }

    public AutoScheduleStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(AutoScheduleStrategy strategy) {
        this.strategy = strategy;
    }

    public String getStrategyVersion() {
        return strategyVersion;
    }

    public void setStrategyVersion(String strategyVersion) {
        this.strategyVersion = strategyVersion;
    }

    public SchedulePreviewApplyMode getApplyMode() {
        return applyMode;
    }

    public void setApplyMode(SchedulePreviewApplyMode applyMode) {
        this.applyMode = applyMode;
    }

    public SchedulePreviewStatus getStatus() {
        return status;
    }

    public void setStatus(SchedulePreviewStatus status) {
        this.status = status;
    }

    public Integer getSlotGranularityMinutes() {
        return slotGranularityMinutes;
    }

    public void setSlotGranularityMinutes(Integer slotGranularityMinutes) {
        this.slotGranularityMinutes = slotGranularityMinutes;
    }

    public Integer getTotalCandidateCount() {
        return totalCandidateCount;
    }

    public void setTotalCandidateCount(Integer totalCandidateCount) {
        this.totalCandidateCount = totalCandidateCount;
    }

    public Integer getValidCandidateCount() {
        return validCandidateCount;
    }

    public void setValidCandidateCount(Integer validCandidateCount) {
        this.validCandidateCount = validCandidateCount;
    }

    public Integer getRejectedCandidateCount() {
        return rejectedCandidateCount;
    }

    public void setRejectedCandidateCount(Integer rejectedCandidateCount) {
        this.rejectedCandidateCount = rejectedCandidateCount;
    }

    public Integer getSelectedCandidateCount() {
        return selectedCandidateCount;
    }

    public void setSelectedCandidateCount(Integer selectedCandidateCount) {
        this.selectedCandidateCount = selectedCandidateCount;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(Long generatedBy) {
        this.generatedBy = generatedBy;
    }

    public Instant getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(Instant appliedAt) {
        this.appliedAt = appliedAt;
    }

    public Long getAppliedBy() {
        return appliedBy;
    }

    public void setAppliedBy(Long appliedBy) {
        this.appliedBy = appliedBy;
    }

    public String getGenerateIdempotencyKey() {
        return generateIdempotencyKey;
    }

    public void setGenerateIdempotencyKey(String generateIdempotencyKey) {
        this.generateIdempotencyKey = generateIdempotencyKey;
    }

    public String getApplyIdempotencyKey() {
        return applyIdempotencyKey;
    }

    public void setApplyIdempotencyKey(String applyIdempotencyKey) {
        this.applyIdempotencyKey = applyIdempotencyKey;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public void setRequestFingerprint(String requestFingerprint) {
        this.requestFingerprint = requestFingerprint;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
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

    public List<ShowtimeSchedulePreviewItem> getItems() {
        return items;
    }

    public void setItems(List<ShowtimeSchedulePreviewItem> items) {
        this.items = items;
    }
}
