package com.project.bookingservice.dto.movie;

public class SeatInfo {
    private Long id;
    private Long roomId;
    private boolean active;

    public SeatInfo() {
    }

    public SeatInfo(Long id, Long roomId, boolean active) {
        this.id = id;
        this.roomId = roomId;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
