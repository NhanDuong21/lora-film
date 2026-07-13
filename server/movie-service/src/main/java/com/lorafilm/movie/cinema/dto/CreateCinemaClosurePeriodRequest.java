package com.lorafilm.movie.cinema.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public class CreateCinemaClosurePeriodRequest {

    @NotNull(message = "Start time is required")
    private Instant startTime;

    @NotNull(message = "End time is required")
    private Instant endTime;

    private String reason;

    public CreateCinemaClosurePeriodRequest() {
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
