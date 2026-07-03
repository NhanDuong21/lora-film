package com.project.movieservice.dto;

import com.project.movieservice.enumtype.ScreenType;
import com.project.movieservice.enumtype.RoomStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RoomCreateRequest {

    @NotBlank(message = "Room name cannot be blank")
    @Size(min = 1, max = 50, message = "Room name must be between 1 and 50 characters")
    private String roomName;

    @NotNull(message = "Total seats cannot be null")
    @Min(value = 1, message = "Total seats must be greater than 0")
    private Integer totalSeats;

    @NotNull(message = "Screen type cannot be null")
    private ScreenType screenType;

    @NotNull(message = "Status cannot be null")
    private RoomStatus status;

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public Integer getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(Integer totalSeats) {
        this.totalSeats = totalSeats;
    }

    public ScreenType getScreenType() {
        return screenType;
    }

    public void setScreenType(ScreenType screenType) {
        this.screenType = screenType;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }
}
