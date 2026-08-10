package com.lorafilm.movie.autoschedule.dto.request;

import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.time.LocalDate;

public class AutoSchedulePreviewHistoryQuery {

    private String cinemaPublicId;

    private SchedulePreviewStatus status;

    private String strategyVersion;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate scheduleFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate scheduleTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant createdFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant createdTo;

    @Min(0)
    private int page = 0;

    @Min(1)
    @Max(100)
    private int size = 10;

    private String sort = "createdAt,desc";

    public String getCinemaPublicId() {
        return cinemaPublicId;
    }

    public void setCinemaPublicId(String cinemaPublicId) {
        this.cinemaPublicId = cinemaPublicId;
    }

    public SchedulePreviewStatus getStatus() {
        return status;
    }

    public void setStatus(SchedulePreviewStatus status) {
        this.status = status;
    }

    public String getStrategyVersion() {
        return strategyVersion;
    }

    public void setStrategyVersion(String strategyVersion) {
        this.strategyVersion = strategyVersion;
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

    public Instant getCreatedFrom() {
        return createdFrom;
    }

    public void setCreatedFrom(Instant createdFrom) {
        this.createdFrom = createdFrom;
    }

    public Instant getCreatedTo() {
        return createdTo;
    }

    public void setCreatedTo(Instant createdTo) {
        this.createdTo = createdTo;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }
}
