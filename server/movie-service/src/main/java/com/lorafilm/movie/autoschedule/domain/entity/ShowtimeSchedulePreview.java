package com.lorafilm.movie.autoschedule.domain.entity;

import com.lorafilm.movie.autoschedule.domain.enums.AutoScheduleStrategy;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewApplyMode;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus;
import com.lorafilm.movie.autoschedule.model.AutoScheduleStrategyVersions;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;
import com.lorafilm.movie.autoschedule.model.AutoScheduleOptimizationResult;
import com.lorafilm.movie.autoschedule.model.AutoScheduleRequestScopeSnapshot;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "showtime_schedule_previews")
public class ShowtimeSchedulePreview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 36, columnDefinition = "CHAR(36)")
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_id", nullable = false)
    private Cinema cinema;

    @Column(name = "schedule_from", nullable = false)
    private LocalDate scheduleFrom;

    @Column(name = "schedule_to", nullable = false)
    private LocalDate scheduleTo;

    @Column(name = "timezone_snapshot", nullable = false, length = 50)
    private String timezoneSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "strategy", nullable = false, length = 30)
    private AutoScheduleStrategy strategy;

    @Column(name = "strategy_version", nullable = false, length = 30)
    private String strategyVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "apply_mode", nullable = false, length = 30)
    private SchedulePreviewApplyMode applyMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
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

    @Column(name = "generate_idempotency_key", nullable = false, unique = true, length = 100)
    private String generateIdempotencyKey;

    @Column(name = "apply_idempotency_key", unique = true, length = 100)
    private String applyIdempotencyKey;

    @Column(name = "request_fingerprint", nullable = false, length = 64, columnDefinition = "CHAR(64)")
    private String requestFingerprint;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_scope_json", columnDefinition = "json")
    private AutoScheduleRequestScopeSnapshot requestScope;

    @Column(name = "policy_version", length = 50)
    private String policyVersion;

    @Column(name = "demand_model_version", length = 64)
    private String demandModelVersion;

    @Column(name = "solver_version", length = 64)
    private String solverVersion;

    @Column(name = "solver_status", length = 30)
    private String solverStatus;

    @Column(name = "eligibility_fingerprint", length = 64, columnDefinition = "CHAR(64)")
    private String eligibilityFingerprint;

    @Column(name = "pricing_fingerprint", length = 64, columnDefinition = "CHAR(64)")
    private String pricingFingerprint;

    @Column(name = "configuration_fingerprint", length = 64, columnDefinition = "CHAR(64)")
    private String configurationFingerprint;

    @Column(name = "objective_value", precision = 19, scale = 3)
    private BigDecimal objectiveValue;

    @Column(name = "objective_best_bound", precision = 19, scale = 3)
    private BigDecimal objectiveBestBound;

    @Column(name = "solver_duration_millis")
    private Long solverDurationMillis;

    @Column(name = "solver_explanation", length = 500)
    private String solverExplanation;

    @Column(name = "expected_attendance", precision = 19, scale = 2)
    private BigDecimal expectedAttendance;

    @Column(name = "expected_occupancy", precision = 12, scale = 6)
    private BigDecimal expectedOccupancy;

    @Column(name = "expected_revenue", precision = 19, scale = 2)
    private BigDecimal expectedRevenue;

    @Column(name = "expected_contribution", precision = 19, scale = 2)
    private BigDecimal expectedContribution;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @org.hibernate.annotations.CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @org.hibernate.annotations.UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "preview", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShowtimeSchedulePreviewItem> items = new ArrayList<>();

    protected ShowtimeSchedulePreview() {}

    public static ShowtimeSchedulePreview createGenerating(Cinema cinema,
                                                           LocalDate scheduleFrom,
                                                           LocalDate scheduleTo,
                                                           Integer slotGranularityMinutes,
                                                           Integer previewTtlMinutes,
                                                           String idempotencyKey,
                                                           String fingerprint,
                                                           Long adminUserId,
                                                           Instant generatedAt) {
        return createGenerating(
                cinema, scheduleFrom, scheduleTo, slotGranularityMinutes, previewTtlMinutes,
                AutoScheduleStrategyVersions.CURRENT, idempotencyKey, fingerprint, adminUserId, generatedAt);
    }

    public static ShowtimeSchedulePreview createGenerating(Cinema cinema,
                                                           LocalDate scheduleFrom,
                                                           LocalDate scheduleTo,
                                                           Integer slotGranularityMinutes,
                                                           Integer previewTtlMinutes,
                                                           String strategyVersion,
                                                           String idempotencyKey,
                                                           String fingerprint,
                                                           Long adminUserId,
                                                           Instant generatedAt) {
        ShowtimeSchedulePreview preview = new ShowtimeSchedulePreview();
        preview.setPublicId(java.util.UUID.randomUUID().toString());
        preview.setCinema(cinema);
        preview.setScheduleFrom(scheduleFrom);
        preview.setScheduleTo(scheduleTo);
        preview.setTimezoneSnapshot(cinema.getTimezone());
        preview.setStrategy(AutoScheduleStrategy.BALANCED);
        preview.setStrategyVersion(strategyVersion);
        preview.setApplyMode(SchedulePreviewApplyMode.ALL_OR_NOTHING);
        preview.setStatus(SchedulePreviewStatus.GENERATING);
        preview.setSlotGranularityMinutes(slotGranularityMinutes);

        preview.setTotalCandidateCount(0);
        preview.setValidCandidateCount(0);
        preview.setRejectedCandidateCount(0);
        preview.setSelectedCandidateCount(0);

        preview.setGeneratedAt(generatedAt);
        preview.setExpiresAt(generatedAt.plus(previewTtlMinutes, java.time.temporal.ChronoUnit.MINUTES));
        preview.setGeneratedBy(adminUserId);

        preview.setGenerateIdempotencyKey(idempotencyKey);
        preview.setRequestFingerprint(fingerprint);
        return preview;
    }

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
        if (this.status != SchedulePreviewStatus.GENERATING) {
            throw new IllegalStateException("Preview must be GENERATING to mark as PREVIEWED");
        }
        this.status = SchedulePreviewStatus.PREVIEWED;
    }

    public void markApplying(String applyIdempotencyKey) {
        if (this.status != SchedulePreviewStatus.PREVIEWED) {
            throw new IllegalStateException("Preview must be PREVIEWED to mark as APPLYING");
        }
        this.status = SchedulePreviewStatus.APPLYING;
        this.applyIdempotencyKey = applyIdempotencyKey;
    }

    public void markApplied(Long actorId, Instant appliedAt) {
        if (this.status != SchedulePreviewStatus.APPLYING) {
            throw new IllegalStateException("Preview must be APPLYING to mark as APPLIED");
        }
        this.status = SchedulePreviewStatus.APPLIED;
        this.appliedBy = actorId;
        this.appliedAt = appliedAt;
    }

    public void markExpired() {
        this.status = SchedulePreviewStatus.EXPIRED;
    }

    public void markCancelled() {
        if (this.status != SchedulePreviewStatus.PREVIEWED
                && this.status != SchedulePreviewStatus.APPLIED) {
            throw new IllegalStateException("Preview must be PREVIEWED or APPLIED to mark as CANCELLED");
        }
        this.status = SchedulePreviewStatus.CANCELLED;
    }

    public void markFailed(String reason) {
        this.status = SchedulePreviewStatus.FAILED;
        this.failureReason = reason;
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

    public AutoScheduleRequestScopeSnapshot getRequestScope() { return requestScope; }
    public void setRequestScope(AutoScheduleRequestScopeSnapshot value) { this.requestScope = value; }
    public String getPolicyVersion() { return policyVersion; }
    public void setPolicyVersion(String value) { this.policyVersion = value; }
    public String getDemandModelVersion() { return demandModelVersion; }
    public void setDemandModelVersion(String value) { this.demandModelVersion = value; }
    public String getSolverVersion() { return solverVersion; }
    public void setSolverVersion(String value) { this.solverVersion = value; }
    public String getSolverStatus() { return solverStatus; }
    public void setSolverStatus(String value) { this.solverStatus = value; }
    public String getEligibilityFingerprint() { return eligibilityFingerprint; }
    public void setEligibilityFingerprint(String value) { this.eligibilityFingerprint = value; }
    public String getPricingFingerprint() { return pricingFingerprint; }
    public void setPricingFingerprint(String value) { this.pricingFingerprint = value; }
    public String getConfigurationFingerprint() { return configurationFingerprint; }
    public void setConfigurationFingerprint(String value) { this.configurationFingerprint = value; }
    public BigDecimal getObjectiveValue() { return objectiveValue; }
    public void setObjectiveValue(BigDecimal value) { this.objectiveValue = value; }
    public BigDecimal getObjectiveBestBound() { return objectiveBestBound; }
    public void setObjectiveBestBound(BigDecimal value) { this.objectiveBestBound = value; }
    public Long getSolverDurationMillis() { return solverDurationMillis; }
    public void setSolverDurationMillis(Long value) { this.solverDurationMillis = value; }
    public String getSolverExplanation() { return solverExplanation; }
    public void setSolverExplanation(String value) { this.solverExplanation = value; }
    public BigDecimal getExpectedAttendance() { return expectedAttendance; }
    public void setExpectedAttendance(BigDecimal value) { this.expectedAttendance = value; }
    public BigDecimal getExpectedOccupancy() { return expectedOccupancy; }
    public void setExpectedOccupancy(BigDecimal value) { this.expectedOccupancy = value; }
    public BigDecimal getExpectedRevenue() { return expectedRevenue; }
    public void setExpectedRevenue(BigDecimal value) { this.expectedRevenue = value; }
    public BigDecimal getExpectedContribution() { return expectedContribution; }
    public void setExpectedContribution(BigDecimal value) { this.expectedContribution = value; }

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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<ShowtimeSchedulePreviewItem> getItems() {
        return items;
    }

    public void setItems(List<ShowtimeSchedulePreviewItem> items) {
        this.items = items;
    }
}
