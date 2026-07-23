package com.lorafilm.movie.autoschedule.dto.response;

import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewApplyMode;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus;

import java.time.Instant;
import java.time.LocalDate;

public class AutoSchedulePreviewHistoryItemResponse {

    private String previewPublicId;
    private Long version;
    private String cinemaPublicId;
    private String cinemaName;
    private String timezoneSnapshot;
    private LocalDate scheduleFrom;
    private LocalDate scheduleTo;
    private String strategyVersion;
    private SchedulePreviewApplyMode applyMode;
    private SchedulePreviewStatus persistedStatus;
    private SchedulePreviewStatus displayStatus;
    private boolean editable;
    private boolean applicable;
    private Integer totalCandidateCount;
    private Integer validCandidateCount;
    private Integer rejectedCandidateCount;
    private Integer selectedCandidateCount;
    private Integer appliedShowtimeCount;
    private Instant createdAt;
    private Instant expiresAt;
    private Instant appliedAt;
    private String failureReasonSafe;

    public String getPreviewPublicId() { return previewPublicId; }
    public void setPreviewPublicId(String previewPublicId) { this.previewPublicId = previewPublicId; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public String getCinemaPublicId() { return cinemaPublicId; }
    public void setCinemaPublicId(String cinemaPublicId) { this.cinemaPublicId = cinemaPublicId; }
    public String getCinemaName() { return cinemaName; }
    public void setCinemaName(String cinemaName) { this.cinemaName = cinemaName; }
    public String getTimezoneSnapshot() { return timezoneSnapshot; }
    public void setTimezoneSnapshot(String timezoneSnapshot) { this.timezoneSnapshot = timezoneSnapshot; }
    public LocalDate getScheduleFrom() { return scheduleFrom; }
    public void setScheduleFrom(LocalDate scheduleFrom) { this.scheduleFrom = scheduleFrom; }
    public LocalDate getScheduleTo() { return scheduleTo; }
    public void setScheduleTo(LocalDate scheduleTo) { this.scheduleTo = scheduleTo; }
    public String getStrategyVersion() { return strategyVersion; }
    public void setStrategyVersion(String strategyVersion) { this.strategyVersion = strategyVersion; }
    public SchedulePreviewApplyMode getApplyMode() { return applyMode; }
    public void setApplyMode(SchedulePreviewApplyMode applyMode) { this.applyMode = applyMode; }
    public SchedulePreviewStatus getPersistedStatus() { return persistedStatus; }
    public void setPersistedStatus(SchedulePreviewStatus persistedStatus) { this.persistedStatus = persistedStatus; }
    public SchedulePreviewStatus getDisplayStatus() { return displayStatus; }
    public void setDisplayStatus(SchedulePreviewStatus displayStatus) { this.displayStatus = displayStatus; }
    public boolean isEditable() { return editable; }
    public void setEditable(boolean editable) { this.editable = editable; }
    public boolean isApplicable() { return applicable; }
    public void setApplicable(boolean applicable) { this.applicable = applicable; }
    public Integer getTotalCandidateCount() { return totalCandidateCount; }
    public void setTotalCandidateCount(Integer totalCandidateCount) { this.totalCandidateCount = totalCandidateCount; }
    public Integer getValidCandidateCount() { return validCandidateCount; }
    public void setValidCandidateCount(Integer validCandidateCount) { this.validCandidateCount = validCandidateCount; }
    public Integer getRejectedCandidateCount() { return rejectedCandidateCount; }
    public void setRejectedCandidateCount(Integer rejectedCandidateCount) { this.rejectedCandidateCount = rejectedCandidateCount; }
    public Integer getSelectedCandidateCount() { return selectedCandidateCount; }
    public void setSelectedCandidateCount(Integer selectedCandidateCount) { this.selectedCandidateCount = selectedCandidateCount; }
    public Integer getAppliedShowtimeCount() { return appliedShowtimeCount; }
    public void setAppliedShowtimeCount(Integer appliedShowtimeCount) { this.appliedShowtimeCount = appliedShowtimeCount; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getAppliedAt() { return appliedAt; }
    public void setAppliedAt(Instant appliedAt) { this.appliedAt = appliedAt; }
    public String getFailureReasonSafe() { return failureReasonSafe; }
    public void setFailureReasonSafe(String failureReasonSafe) { this.failureReasonSafe = failureReasonSafe; }
}
