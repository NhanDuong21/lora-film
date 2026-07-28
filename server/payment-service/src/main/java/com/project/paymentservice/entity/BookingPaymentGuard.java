package com.project.paymentservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "booking_payment_guards")
public class BookingPaymentGuard {

    @Id
    @Column(name = "booking_public_id", nullable = false, columnDefinition = "char(36)")
    private String bookingPublicId;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "active_payment_id")
    private Long activePaymentId;

    @Column(name = "successful_payment_id")
    private Long successfulPaymentId;

    @Column(name = "next_attempt_number", nullable = false)
    private Integer nextAttemptNumber = 1;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    public BookingPaymentGuard() {
    }

    public String getBookingPublicId() { return bookingPublicId; }
    public void setBookingPublicId(String bookingPublicId) { this.bookingPublicId = bookingPublicId; }
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    public Long getActivePaymentId() { return activePaymentId; }
    public void setActivePaymentId(Long activePaymentId) { this.activePaymentId = activePaymentId; }
    public Long getSuccessfulPaymentId() { return successfulPaymentId; }
    public void setSuccessfulPaymentId(Long successfulPaymentId) { this.successfulPaymentId = successfulPaymentId; }
    public Integer getNextAttemptNumber() { return nextAttemptNumber; }
    public void setNextAttemptNumber(Integer nextAttemptNumber) { this.nextAttemptNumber = nextAttemptNumber; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
