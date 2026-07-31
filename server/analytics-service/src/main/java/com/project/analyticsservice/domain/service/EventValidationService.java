package com.project.analyticsservice.domain.service;

import com.project.analyticsservice.exception.NonRetryableAnalyticsEventException;
import com.project.analyticsservice.kafka.event.BookingCancelledEvent;
import com.project.analyticsservice.kafka.event.PaymentRefundedEvent;
import com.project.analyticsservice.kafka.event.PaymentSucceededEvent;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Set;

@Service
public class EventValidationService {
    private static final String VERSION = "1.0";
    private static final Set<String> CANCELLATION_REASONS = Set.of(
            "USER_CANCELLED", "PAYMENT_TIMEOUT", "ADMIN_CANCELLED",
            "SYSTEM_CANCELLED", "PAYMENT_FAILED");

    public void validate(PaymentSucceededEvent event) {
        require(event != null, "Payment event is required");
        requireText(event.eventId(), "eventId");
        require(VERSION.equals(event.schemaVersion()), "Unsupported payment schemaVersion");
        requireText(event.paymentPublicId(), "paymentPublicId");
        requireText(event.bookingPublicId(), "bookingPublicId");
        requireText(event.provider(), "provider");
        requireText(event.currency(), "currency");
        require(event.currency().matches("[A-Z]{3}"), "currency must be an uppercase ISO-4217 code");
        require("VND".equals(event.currency()), "analytics currently supports VND only");
        require(event.succeededAt() != null, "succeededAt is required");
        requireText(event.movieTitle(), "movieTitle");
        require(event.ticketCount() != null && event.ticketCount() >= 0, "ticketCount must be non-negative");
        requireMoney(event.amount(), "amount");
        requireMoney(event.ticketAmount(), "ticketAmount");
        requireMoney(event.foodAmount(), "foodAmount");
        requireMoney(event.discountAmount(), "discountAmount");
        requireMoney(event.totalAmount(), "totalAmount");

        BigDecimal gross = event.ticketAmount().add(event.foodAmount());
        require(event.amount().compareTo(event.totalAmount()) == 0, "amount must equal totalAmount");
        require(gross.subtract(event.discountAmount()).compareTo(event.totalAmount()) == 0,
                "ticketAmount + foodAmount - discountAmount must equal totalAmount");
        require(event.availableSeats() == null || event.availableSeats() >= event.ticketCount(),
                "availableSeats cannot be lower than ticketCount");
    }

    public void validate(BookingCancelledEvent event) {
        require(event != null, "Cancellation event is required");
        requireText(event.eventId(), "eventId");
        require("BOOKING_CANCELLED".equals(event.eventType()), "eventType must be BOOKING_CANCELLED");
        require(VERSION.equals(event.eventVersion()), "Unsupported cancellation eventVersion");
        require("booking-service".equals(event.sourceService()), "sourceService must be booking-service");
        require(event.occurredAt() != null, "occurredAt is required");
        require((event.bookingId() != null && event.bookingId() > 0)
                        || StringUtils.hasText(event.bookingPublicId()),
                "A positive bookingId or bookingPublicId is required");
        requireText(event.previousStatus(), "previousStatus");
        require("CANCELLED".equals(event.currentStatus()), "currentStatus must be CANCELLED");
        require(CANCELLATION_REASONS.contains(event.reason()), "Unsupported cancellation reason");
    }

    public void validate(PaymentRefundedEvent event) {
        require(event != null, "Refund event is required");
        requireText(event.eventId(), "eventId");
        require(VERSION.equals(event.schemaVersion()), "Unsupported refund schemaVersion");
        require(event.eventType() == null || "PAYMENT_REFUNDED".equals(event.eventType()),
                "eventType must be PAYMENT_REFUNDED");
        require(event.sourceService() == null || "payment-service".equals(event.sourceService()),
                "sourceService must be payment-service");
        requireText(event.paymentPublicId(), "paymentPublicId");
        requireText(event.bookingPublicId(), "bookingPublicId");
        requireText(event.currency(), "currency");
        require(event.currency().matches("[A-Z]{3}"), "currency must be an uppercase ISO-4217 code");
        require("VND".equals(event.currency()), "analytics currently supports VND only");
        requireMoney(event.refundAmount(), "refundAmount");
        require(event.refundAmount().signum() > 0, "refundAmount must be greater than zero");
        require(event.refundedAt() != null, "refundedAt is required");
    }

    private void requireMoney(BigDecimal value, String field) {
        require(value != null && value.signum() >= 0, field + " must be non-negative");
    }

    private void requireText(String value, String field) {
        require(StringUtils.hasText(value), field + " is required");
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new NonRetryableAnalyticsEventException(message);
        }
    }
}
