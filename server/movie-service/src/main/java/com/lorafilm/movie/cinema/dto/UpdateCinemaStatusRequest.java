package com.lorafilm.movie.cinema.dto;

import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateCinemaStatusRequest {

    @NotNull(message = "Status is required")
    private CinemaStatus status;

    public UpdateCinemaStatusRequest() {
    }

    public CinemaStatus getStatus() {
        return status;
    }

    public void setStatus(CinemaStatus status) {
        this.status = status;
    }
}
