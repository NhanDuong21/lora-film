package com.project.movieservice.dto;

import com.project.movieservice.enumtype.RoomStatus;
import jakarta.validation.constraints.NotNull;

public class RoomStatusUpdateRequest {

    @NotNull(message = "Status cannot be null")
    private RoomStatus status;

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }
}
