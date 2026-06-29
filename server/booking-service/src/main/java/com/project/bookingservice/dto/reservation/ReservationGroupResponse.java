package com.project.bookingservice.dto.reservation;

import com.project.bookingservice.enumtype.ReservationStatus;
import java.time.LocalDateTime;
import java.util.List;

public class ReservationGroupResponse {
    private Long showtimeId;
    private Long userId;
    private ReservationStatus status;
    private LocalDateTime expiresAt;
    private List<ReservationSeatResponse> reservations;

    public ReservationGroupResponse() {}

    public ReservationGroupResponse(Long showtimeId, Long userId, ReservationStatus status, LocalDateTime expiresAt, List<ReservationSeatResponse> reservations) {
        this.showtimeId = showtimeId;
        this.userId = userId;
        this.status = status;
        this.expiresAt = expiresAt;
        this.reservations = reservations;
    }

    public Long getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(Long showtimeId) {
        this.showtimeId = showtimeId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public List<ReservationSeatResponse> getReservations() {
        return reservations;
    }

    public void setReservations(List<ReservationSeatResponse> reservations) {
        this.reservations = reservations;
    }
}
