package com.lorafilm.booking.domain.entity;

import com.lorafilm.booking.domain.enums.BookingSource;
import com.lorafilm.booking.domain.enums.BookingStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
public class Booking extends BaseEntity {

    @Column(name = "booking_code", length = 50, nullable = false, unique = true)
    private String bookingCode;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "showtime_id", nullable = false)
    private Long showtimeId;

    @Column(name = "total_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "currency", length = 10, nullable = false)
    private String currency = "VND";

    @Column(name = "ticket_count", nullable = false)
    private Integer ticketCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingStatus status = BookingStatus.PENDING_PAYMENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_source", nullable = false)
    private BookingSource bookingSource = BookingSource.WEB;

    @Column(name = "payment_deadline", nullable = false)
    private Instant paymentDeadline;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "expired_at")
    private Instant expiredAt;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @Column(name = "note", length = 500)
    private String note;

    @OneToMany(mappedBy = "booking", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<BookingTicket> tickets = new ArrayList<>();

    @OneToMany(mappedBy = "booking", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<BookingStatusHistory> statusHistories = new ArrayList<>();

    @OneToMany(mappedBy = "booking", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<SeatReservation> seatReservations = new ArrayList<>();

    @OneToMany(mappedBy = "booking", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<BookingPaymentEvent> paymentEvents = new ArrayList<>();

    @OneToMany(mappedBy = "booking", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<BookingReconciliationTask> reconciliationTasks = new ArrayList<>();

    @OneToMany(mappedBy = "booking", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<BookingAuditLog> auditLogs = new ArrayList<>();

    @OneToMany(mappedBy = "booking", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<BookingSnapshot> snapshots = new ArrayList<>();

    public Booking() {
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public void setBookingCode(String bookingCode) {
        this.bookingCode = bookingCode;
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

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getTicketCount() {
        return ticketCount;
    }

    public void setTicketCount(Integer ticketCount) {
        this.ticketCount = ticketCount;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public BookingSource getBookingSource() {
        return bookingSource;
    }

    public void setBookingSource(BookingSource bookingSource) {
        this.bookingSource = bookingSource;
    }

    public Instant getPaymentDeadline() {
        return paymentDeadline;
    }

    public void setPaymentDeadline(Instant paymentDeadline) {
        this.paymentDeadline = paymentDeadline;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public Instant getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(Instant expiredAt) {
        this.expiredAt = expiredAt;
    }

    public Instant getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(Instant refundedAt) {
        this.refundedAt = refundedAt;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<BookingTicket> getTickets() {
        return tickets;
    }

    public void setTickets(List<BookingTicket> tickets) {
        this.tickets = tickets;
    }

    public List<BookingStatusHistory> getStatusHistories() {
        return statusHistories;
    }

    public void setStatusHistories(List<BookingStatusHistory> statusHistories) {
        this.statusHistories = statusHistories;
    }

    public List<SeatReservation> getSeatReservations() {
        return seatReservations;
    }

    public void setSeatReservations(List<SeatReservation> seatReservations) {
        this.seatReservations = seatReservations;
    }

    public List<BookingPaymentEvent> getPaymentEvents() {
        return paymentEvents;
    }

    public void setPaymentEvents(List<BookingPaymentEvent> paymentEvents) {
        this.paymentEvents = paymentEvents;
    }

    public List<BookingReconciliationTask> getReconciliationTasks() {
        return reconciliationTasks;
    }

    public void setReconciliationTasks(List<BookingReconciliationTask> reconciliationTasks) {
        this.reconciliationTasks = reconciliationTasks;
    }

    public List<BookingAuditLog> getAuditLogs() {
        return auditLogs;
    }

    public void setAuditLogs(List<BookingAuditLog> auditLogs) {
        this.auditLogs = auditLogs;
    }

    public List<BookingSnapshot> getSnapshots() {
        return snapshots;
    }

    public void setSnapshots(List<BookingSnapshot> snapshots) {
        this.snapshots = snapshots;
    }
}
