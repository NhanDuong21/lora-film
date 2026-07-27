package com.lorafilm.booking.booking.dto;

import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class BookingDetailResponse {

    private Long id;
    private String publicId;
    private String bookingCode;
    private Long userId;
    private Long showtimeId;
    private String showtimePublicId;
    private Long movieId;
    private Long cinemaId;
    private Long auditoriumId;
    private BigDecimal ticketAmount;
    private BigDecimal foodAmount;
    private BigDecimal serviceFee;
    private BigDecimal taxAmount;
    private BigDecimal promotionDiscount;
    private BigDecimal voucherDiscount;
    private BigDecimal finalAmount;
    private String currency;
    private BookingStatus bookingStatus;
    private PaymentStatus paymentStatus;
    private String paymentMethodSnapshot;
    private String paymentProvider;
    private String paymentReference;
    private Instant expiresAt;
    private Instant amountLockedAt;
    private Instant confirmedAt;
    private Instant completedAt;
    private Instant cancelledAt;
    private Instant expiredAt;
    private Instant refundedAt;
    private String cancelReasonCode;
    private String cancelReasonDetail;
    private String note;
    private Instant createdAt;

    private BookingSnapshotDto snapshot;
    private List<BookingTicketDto> tickets;
    private List<BookingReservationAdminDto> reservations;
    private BookingOperationalInfoDto operationalInfo;
    private List<BookingStatusHistoryDto> statusHistories;

    public BookingDetailResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
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

    public String getShowtimePublicId() {
        return showtimePublicId;
    }

    public void setShowtimePublicId(String showtimePublicId) {
        this.showtimePublicId = showtimePublicId;
    }

    public Long getMovieId() {
        return movieId;
    }

    public void setMovieId(Long movieId) {
        this.movieId = movieId;
    }

    public Long getCinemaId() {
        return cinemaId;
    }

    public void setCinemaId(Long cinemaId) {
        this.cinemaId = cinemaId;
    }

    public Long getAuditoriumId() {
        return auditoriumId;
    }

    public void setAuditoriumId(Long auditoriumId) {
        this.auditoriumId = auditoriumId;
    }

    public BigDecimal getTicketAmount() {
        return ticketAmount;
    }

    public void setTicketAmount(BigDecimal ticketAmount) {
        this.ticketAmount = ticketAmount;
    }

    public BigDecimal getFoodAmount() {
        return foodAmount;
    }

    public void setFoodAmount(BigDecimal foodAmount) {
        this.foodAmount = foodAmount;
    }

    public BigDecimal getServiceFee() {
        return serviceFee;
    }

    public void setServiceFee(BigDecimal serviceFee) {
        this.serviceFee = serviceFee;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getPromotionDiscount() {
        return promotionDiscount;
    }

    public void setPromotionDiscount(BigDecimal promotionDiscount) {
        this.promotionDiscount = promotionDiscount;
    }

    public BigDecimal getVoucherDiscount() {
        return voucherDiscount;
    }

    public void setVoucherDiscount(BigDecimal voucherDiscount) {
        this.voucherDiscount = voucherDiscount;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BookingStatus getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(BookingStatus bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentMethodSnapshot() {
        return paymentMethodSnapshot;
    }

    public void setPaymentMethodSnapshot(String paymentMethodSnapshot) {
        this.paymentMethodSnapshot = paymentMethodSnapshot;
    }

    public String getPaymentProvider() {
        return paymentProvider;
    }

    public void setPaymentProvider(String paymentProvider) {
        this.paymentProvider = paymentProvider;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getAmountLockedAt() {
        return amountLockedAt;
    }

    public void setAmountLockedAt(Instant amountLockedAt) {
        this.amountLockedAt = amountLockedAt;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
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

    public String getCancelReasonCode() {
        return cancelReasonCode;
    }

    public void setCancelReasonCode(String cancelReasonCode) {
        this.cancelReasonCode = cancelReasonCode;
    }

    public String getCancelReasonDetail() {
        return cancelReasonDetail;
    }

    public void setCancelReasonDetail(String cancelReasonDetail) {
        this.cancelReasonDetail = cancelReasonDetail;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public BookingSnapshotDto getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(BookingSnapshotDto snapshot) {
        this.snapshot = snapshot;
    }

    public List<BookingTicketDto> getTickets() {
        return tickets;
    }

    public void setTickets(List<BookingTicketDto> tickets) {
        this.tickets = tickets;
    }

    public List<BookingReservationAdminDto> getReservations() {
        return reservations;
    }

    public void setReservations(List<BookingReservationAdminDto> reservations) {
        this.reservations = reservations;
    }

    public BookingOperationalInfoDto getOperationalInfo() {
        return operationalInfo;
    }

    public void setOperationalInfo(BookingOperationalInfoDto operationalInfo) {
        this.operationalInfo = operationalInfo;
    }

    public List<BookingStatusHistoryDto> getStatusHistories() {
        return statusHistories;
    }

    public void setStatusHistories(List<BookingStatusHistoryDto> statusHistories) {
        this.statusHistories = statusHistories;
    }
}
