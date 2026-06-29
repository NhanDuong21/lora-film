package com.project.bookingservice.dto.movie;

public class ShowtimeInfo {
    private Long id;
    private Long roomId;
    private boolean available;
    private java.math.BigDecimal price;

    public ShowtimeInfo() {
    }

    public ShowtimeInfo(Long id, Long roomId, boolean available, java.math.BigDecimal price) {
        this.id = id;
        this.roomId = roomId;
        this.available = available;
        this.price = price;
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

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public java.math.BigDecimal getPrice() {
        return price;
    }

    public void setPrice(java.math.BigDecimal price) {
        this.price = price;
    }
}
