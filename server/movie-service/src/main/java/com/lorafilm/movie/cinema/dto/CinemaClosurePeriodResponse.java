package com.lorafilm.movie.cinema.dto;

import com.lorafilm.movie.common.enums.ActionStatus;
import java.time.Instant;
import java.time.LocalDate;

public class CinemaClosurePeriodResponse {

    private Long id;
    private String cinemaPublicId;
    private Instant startTime;
    private Instant endTime;
    private LocalDate serviceDate;
    private String reason;
    private ActionStatus status;

    public CinemaClosurePeriodResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCinemaPublicId() {
        return cinemaPublicId;
    }

    public void setCinemaPublicId(String cinemaPublicId) {
        this.cinemaPublicId = cinemaPublicId;
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

    public LocalDate getServiceDate() {
        return serviceDate;
    }

    public void setServiceDate(LocalDate serviceDate) {
        this.serviceDate = serviceDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public ActionStatus getStatus() {
        return status;
    }

    public void setStatus(ActionStatus status) {
        this.status = status;
    }
}
