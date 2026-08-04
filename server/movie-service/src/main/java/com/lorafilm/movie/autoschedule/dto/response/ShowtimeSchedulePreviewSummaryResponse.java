package com.lorafilm.movie.autoschedule.dto.response;

import com.lorafilm.movie.autoschedule.domain.enums.AutoScheduleStrategy;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewApplyMode;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;

public class ShowtimeSchedulePreviewSummaryResponse {

    private String previewPublicId;
    private Long version;

    private String cinemaPublicId;
    private String cinemaSlug;
    private String cinemaName;

    private LocalDate scheduleFrom;
    private LocalDate scheduleTo;
    private String timezoneSnapshot;

    private AutoScheduleStrategy strategy;
    private String strategyVersion;
    private SchedulePreviewApplyMode applyMode;
    private SchedulePreviewStatus status;

    private Integer slotGranularityMinutes;

    private Integer totalCandidateCount;
    private Integer validCandidateCount;
    private Integer rejectedCandidateCount;
    private Integer selectedCandidateCount;

    private Instant generatedAt;
    private Instant expiresAt;
    private Long generatedBy;

    private Instant appliedAt;
    private Long appliedBy;

    private String failureReason;

    public ShowtimeSchedulePreviewSummaryResponse() {
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
    public String getPreviewPublicId() { return previewPublicId; }
    public void setPreviewPublicId(String previewPublicId) { this.previewPublicId = previewPublicId; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public String getCinemaPublicId() { return cinemaPublicId; }
    public void setCinemaPublicId(String cinemaPublicId) { this.cinemaPublicId = cinemaPublicId; }

    public String getCinemaSlug() { return cinemaSlug; }
    public void setCinemaSlug(String cinemaSlug) { this.cinemaSlug = cinemaSlug; }

    public String getCinemaName() { return cinemaName; }
    public void setCinemaName(String cinemaName) { this.cinemaName = cinemaName; }

    public LocalDate getScheduleFrom() { return scheduleFrom; }
    public void setScheduleFrom(LocalDate scheduleFrom) { this.scheduleFrom = scheduleFrom; }

    public LocalDate getScheduleTo() { return scheduleTo; }
    public void setScheduleTo(LocalDate scheduleTo) { this.scheduleTo = scheduleTo; }

    public String getTimezoneSnapshot() { return timezoneSnapshot; }
    public void setTimezoneSnapshot(String timezoneSnapshot) { this.timezoneSnapshot = timezoneSnapshot; }

    public AutoScheduleStrategy getStrategy() { return strategy; }
    public void setStrategy(AutoScheduleStrategy strategy) { this.strategy = strategy; }

    public String getStrategyVersion() { return strategyVersion; }
    public void setStrategyVersion(String strategyVersion) { this.strategyVersion = strategyVersion; }

    public SchedulePreviewApplyMode getApplyMode() { return applyMode; }
    public void setApplyMode(SchedulePreviewApplyMode applyMode) { this.applyMode = applyMode; }

    public SchedulePreviewStatus getStatus() { return status; }
    public void setStatus(SchedulePreviewStatus status) { this.status = status; }

    public Integer getSlotGranularityMinutes() { return slotGranularityMinutes; }
    public void setSlotGranularityMinutes(Integer slotGranularityMinutes) { this.slotGranularityMinutes = slotGranularityMinutes; }

    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Long getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(Long generatedBy) { this.generatedBy = generatedBy; }

    public Instant getAppliedAt() { return appliedAt; }
    public void setAppliedAt(Instant appliedAt) { this.appliedAt = appliedAt; }

    public Long getAppliedBy() { return appliedBy; }
    public void setAppliedBy(Long appliedBy) { this.appliedBy = appliedBy; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}
