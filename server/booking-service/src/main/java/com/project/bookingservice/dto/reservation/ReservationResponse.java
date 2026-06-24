package com.project.bookingservice.dto.reservation;

import com.project.bookingservice.enumtype.ReservationStatus;
import java.time.LocalDateTime;

public class ReservationResponse {

    private Long id;
    private Long showtimeId;
    private Long seatId;
    private ReservationStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    public ReservationResponse() {
    }

    public ReservationResponse(Long id, Long showtimeId, Long seatId, ReservationStatus status, LocalDateTime expiresAt, LocalDateTime createdAt) {
        this.id = id;
        this.showtimeId = showtimeId;
        this.seatId = seatId;
        this.status = status;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(Long showtimeId) {
        this.showtimeId = showtimeId;
    }

    public Long getSeatId() {
        return seatId;
    }

    public void setSeatId(Long seatId) {
        this.seatId = seatId;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
