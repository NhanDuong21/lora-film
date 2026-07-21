package com.lorafilm.booking.domain.entity;

import com.lorafilm.booking.domain.enums.BookingStatus;
import com.lorafilm.booking.domain.enums.StatusChangeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "booking_status_histories")
public class BookingStatusHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status")
    private BookingStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false)
    private BookingStatus currentStatus;

    @Column(name = "changed_by")
    private Long changedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false)
    private StatusChangeType changeType = StatusChangeType.SYSTEM;

    @Column(name = "reason", length = 500)
    private String reason;

    public BookingStatusHistory() {
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public BookingStatus getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(BookingStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    public BookingStatus getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(BookingStatus currentStatus) {
        this.currentStatus = currentStatus;
    }

    public Long getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(Long changedBy) {
        this.changedBy = changedBy;
    }

    public StatusChangeType getChangeType() {
        return changeType;
    }

    public void setChangeType(StatusChangeType changeType) {
        this.changeType = changeType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
