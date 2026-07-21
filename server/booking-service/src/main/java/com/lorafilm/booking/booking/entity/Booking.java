package com.lorafilm.booking.booking.entity;

import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.enums.PaymentStatus;
import com.lorafilm.booking.common.entity.FullAuditableEntity;
import com.lorafilm.booking.common.exception.InvalidBookingStatusException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "bookings")
public class Booking extends FullAuditableEntity {

    @Column(name = "booking_code", length = 50, nullable = false, unique = true)
    private String bookingCode;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "showtime_id", nullable = false)
    private Long showtimeId;

    @Column(name = "movie_id", nullable = false)
    private Long movieId;

    @Column(name = "cinema_id", nullable = false)
    private Long cinemaId;

    @Column(name = "auditorium_id", nullable = false)
    private Long auditoriumId;

    @Column(name = "ticket_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal ticketAmount = BigDecimal.ZERO;

    @Column(name = "food_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal foodAmount = BigDecimal.ZERO;

    @Column(name = "service_fee", precision = 12, scale = 2, nullable = false)
    private BigDecimal serviceFee = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "promotion_discount", precision = 12, scale = 2, nullable = false)
    private BigDecimal promotionDiscount = BigDecimal.ZERO;

    @Column(name = "voucher_discount", precision = 12, scale = 2, nullable = false)
    private BigDecimal voucherDiscount = BigDecimal.ZERO;

    @Column(name = "final_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal finalAmount;

    @Column(name = "currency", length = 10, nullable = false)
    private String currency = "VND";

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status", nullable = false)
    private BookingStatus bookingStatus = BookingStatus.PENDING_PAYMENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "payment_method_snapshot", length = 50)
    private String paymentMethodSnapshot;

    @Column(name = "payment_provider", length = 50)
    private String paymentProvider;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "expired_at")
    private Instant expiredAt;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    @Column(name = "cancel_reason_code", length = 50)
    private String cancelReasonCode;

    @Column(name = "cancel_reason_detail", columnDefinition = "TEXT")
    private String cancelReasonDetail;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    public Booking() {
    }

    public static Booking create(
            String publicId,
            String bookingCode,
            Long userId,
            Long showtimeId,
            Long movieId,
            Long cinemaId,
            Long auditoriumId,
            BigDecimal ticketAmount,
            BigDecimal foodAmount,
            BigDecimal serviceFee,
            BigDecimal taxAmount,
            BigDecimal promotionDiscount,
            BigDecimal voucherDiscount,
            String currency,
            Instant expiresAt,
            String note) {
        Booking booking = new Booking();
        booking.setPublicId(Objects.requireNonNull(publicId, "publicId is required"));
        booking.bookingCode = requireText(bookingCode, "bookingCode");
        booking.userId = requirePositive(userId, "userId");
        booking.showtimeId = requirePositive(showtimeId, "showtimeId");
        booking.movieId = requirePositive(movieId, "movieId");
        booking.cinemaId = requirePositive(cinemaId, "cinemaId");
        booking.auditoriumId = requirePositive(auditoriumId, "auditoriumId");
        booking.ticketAmount = requireNonNegative(ticketAmount, "ticketAmount");
        booking.foodAmount = requireNonNegative(foodAmount, "foodAmount");
        booking.serviceFee = requireNonNegative(serviceFee, "serviceFee");
        booking.taxAmount = requireNonNegative(taxAmount, "taxAmount");
        booking.promotionDiscount = requireNonNegative(promotionDiscount, "promotionDiscount");
        booking.voucherDiscount = requireNonNegative(voucherDiscount, "voucherDiscount");
        booking.currency = requireText(currency, "currency");
        booking.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt is required");
        booking.note = note;
        booking.bookingStatus = BookingStatus.PENDING_PAYMENT;
        booking.paymentStatus = PaymentStatus.PENDING;
        booking.recalculateFinalAmount();
        return booking;
    }

    public void changeStatus(BookingStatus targetStatus, Instant changedAt) {
        Objects.requireNonNull(targetStatus, "targetStatus is required");
        Objects.requireNonNull(changedAt, "changedAt is required");

        if (!bookingStatus.canTransitionTo(targetStatus)) {
            throw new InvalidBookingStatusException(
                    "Cannot change booking status from " + bookingStatus + " to " + targetStatus);
        }
        if (targetStatus == BookingStatus.CONFIRMED && !changedAt.isBefore(expiresAt)) {
            throw new InvalidBookingStatusException("Expired booking cannot be confirmed");
        }
        if (targetStatus == BookingStatus.EXPIRED && changedAt.isBefore(expiresAt)) {
            throw new InvalidBookingStatusException("Booking cannot expire before its payment deadline");
        }

        bookingStatus = targetStatus;
        switch (targetStatus) {
            case CONFIRMED -> confirmedAt = changedAt;
            case COMPLETED -> completedAt = changedAt;
            case CANCELLED -> cancelledAt = changedAt;
            case EXPIRED -> expiredAt = changedAt;
            case REFUNDED -> refundedAt = changedAt;
            case PENDING_PAYMENT -> throw new InvalidBookingStatusException(
                    "Cannot transition a booking back to PENDING_PAYMENT");
        }
    }

    public void cancel(String reasonCode, String reasonDetail, Instant cancelledAt) {
        changeStatus(BookingStatus.CANCELLED, cancelledAt);
        this.cancelReasonCode = reasonCode;
        this.cancelReasonDetail = reasonDetail;
    }

    private void recalculateFinalAmount() {
        BigDecimal grossAmount = ticketAmount.add(foodAmount).add(serviceFee).add(taxAmount);
        BigDecimal totalDiscount = promotionDiscount.add(voucherDiscount);
        finalAmount = grossAmount.subtract(totalDiscount);
        if (finalAmount.signum() < 0) {
            throw new IllegalArgumentException("finalAmount cannot be negative");
        }
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static BigDecimal requireNonNegative(BigDecimal value, String fieldName) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(fieldName + " must be non-negative");
        }
        return value;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
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
}
