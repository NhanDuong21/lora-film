package com.lorafilm.movie.autoschedule.dto.response;

import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus;

import java.time.Instant;
import java.util.List;

public class ApplyShowtimeSchedulePreviewResponse {

    private String previewPublicId;
    private Long version;
    private SchedulePreviewStatus status;

    private String cinemaPublicId;
    private String cinemaName;

    private Integer createdShowtimeCount;
    private Integer skippedItemCount;

    private Instant appliedAt;
    private Long appliedBy;

    private List<AppliedShowtimeResponse> createdShowtimes;

    public String getPreviewPublicId() {
        return previewPublicId;
    }

    public void setPreviewPublicId(String previewPublicId) {
        this.previewPublicId = previewPublicId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public SchedulePreviewStatus getStatus() {
        return status;
    }

    public void setStatus(SchedulePreviewStatus status) {
        this.status = status;
    }

    public String getCinemaPublicId() {
        return cinemaPublicId;
    }

    public void setCinemaPublicId(String cinemaPublicId) {
        this.cinemaPublicId = cinemaPublicId;
    }

    public String getCinemaName() {
        return cinemaName;
    }

    public void setCinemaName(String cinemaName) {
        this.cinemaName = cinemaName;
    }

    public Integer getCreatedShowtimeCount() {
        return createdShowtimeCount;
    }

    public void setCreatedShowtimeCount(Integer createdShowtimeCount) {
        this.createdShowtimeCount = createdShowtimeCount;
    }

    public Integer getSkippedItemCount() {
        return skippedItemCount;
    }

    public void setSkippedItemCount(Integer skippedItemCount) {
        this.skippedItemCount = skippedItemCount;
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

    public List<AppliedShowtimeResponse> getCreatedShowtimes() {
        return createdShowtimes;
    }

    public void setCreatedShowtimes(List<AppliedShowtimeResponse> createdShowtimes) {
        this.createdShowtimes = createdShowtimes;
    }
}
