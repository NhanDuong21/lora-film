package com.lorafilm.booking.reservation.entity;

import com.lorafilm.booking.common.entity.BaseVersionedEntity;
import com.lorafilm.booking.reservation.enums.ReservationSource;
import com.lorafilm.booking.reservation.enums.SeatReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;

@Entity
@Table(name = "seat_reservations")
public class SeatReservation extends BaseVersionedEntity {

    @Column(name = "reservation_code", length = 50, nullable = false, unique = true)
    private String reservationCode;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "showtime_id", nullable = false)
    private Long showtimeId;

    @Column(name = "seat_id", nullable = false)
    private Long seatId;

    @Column(name = "seat_label", length = 20, nullable = false)
    private String seatLabel;

    @Column(name = "seat_type", length = 30)
    private String seatType;

    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_source", nullable = false)
    private ReservationSource reservationSource = ReservationSource.WEB;

    @Column(name = "redis_lock_key", length = 255)
    private String redisLockKey;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "reserved_at", nullable = false)
    private Instant reservedAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SeatReservationStatus status = SeatReservationStatus.HELD;

    @Column(name = "expired_reason", length = 255)
    private String expiredReason;

    @Column(name = "booking_id")
    private Long bookingId;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    public SeatReservation() {
    }

    public String getReservationCode() {
        return reservationCode;
    }

    public void setReservationCode(String reservationCode) {
        this.reservationCode = reservationCode;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public String getSeatLabel() {
        return seatLabel;
    }

    public void setSeatLabel(String seatLabel) {
        this.seatLabel = seatLabel;
    }

    public String getSeatType() {
        return seatType;
    }

    public void setSeatType(String seatType) {
        this.seatType = seatType;
    }

    public ReservationSource getReservationSource() {
        return reservationSource;
    }

    public void setReservationSource(ReservationSource reservationSource) {
        this.reservationSource = reservationSource;
    }

    public String getRedisLockKey() {
        return redisLockKey;
    }

    public void setRedisLockKey(String redisLockKey) {
        this.redisLockKey = redisLockKey;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getReservedAt() {
        return reservedAt;
    }

    public void setReservedAt(Instant reservedAt) {
        this.reservedAt = reservedAt;
    }

    public SeatReservationStatus getStatus() {
        return status;
    }

    public void setStatus(SeatReservationStatus status) {
        this.status = status;
    }

    public String getExpiredReason() {
        return expiredReason;
    }

    public void setExpiredReason(String expiredReason) {
        this.expiredReason = expiredReason;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
