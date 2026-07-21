package com.lorafilm.booking.domain.entity;

import com.lorafilm.booking.domain.enums.SeatReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "seat_reservations")
public class SeatReservation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Column(name = "showtime_id", nullable = false)
    private Long showtimeId;

    @Column(name = "seat_id", nullable = false)
    private Long seatId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SeatReservationStatus status = SeatReservationStatus.HELD;

    @Column(name = "reservation_token", columnDefinition = "BINARY(16)", nullable = false, unique = true)
    private UUID reservationToken;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "converted_at")
    private Instant convertedAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Column(name = "expired_at")
    private Instant expiredAt;

    @Column(name = "release_reason", length = 255)
    private String releaseReason;

    public SeatReservation() {
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public SeatReservationStatus getStatus() {
        return status;
    }

    public void setStatus(SeatReservationStatus status) {
        this.status = status;
    }

    public UUID getReservationToken() {
        return reservationToken;
    }

    public void setReservationToken(UUID reservationToken) {
        this.reservationToken = reservationToken;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getConvertedAt() {
        return convertedAt;
    }

    public void setConvertedAt(Instant convertedAt) {
        this.convertedAt = convertedAt;
    }

    public Instant getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(Instant releasedAt) {
        this.releasedAt = releasedAt;
    }

    public Instant getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(Instant expiredAt) {
        this.expiredAt = expiredAt;
    }

    public String getReleaseReason() {
        return releaseReason;
    }

    public void setReleaseReason(String releaseReason) {
        this.releaseReason = releaseReason;
    }
}
