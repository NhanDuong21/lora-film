package com.lorafilm.booking.payment.event;

import com.lorafilm.booking.booking.dto.request.InternalPaymentResultRequest;
import com.lorafilm.booking.booking.service.InternalBookingPaymentService;
import com.lorafilm.booking.payment.event.contract.PaymentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Delivery adapter for payment events.
 *
 * <p>The consumer must not contain a second Booking lifecycle.  All payment
 * results are normalized here and handed to the same service used by the
 * internal HTTP receiver.  This keeps duplicate delivery, deadline checks and
 * reservation transitions in one place.</p>
 */
@Service
public class PaymentEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventProcessor.class);

    private final InternalBookingPaymentService paymentService;

    public PaymentEventProcessor(InternalBookingPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public void process(PaymentEvent event, String rawPayload) {
        if (event == null || event.payload() == null || event.payload().bookingId() == null) {
            log.warn("Ignoring payment event without a Booking id: {}", event == null ? null : event.eventId());
            return;
        }

        String result = normalizeResult(event.eventType(), event.payload().paymentStatus());
        // Refund settlement is intentionally owned by Payment.  Recording a
        // refund does not release BOOKED capacity, so there is no lifecycle
        // command to execute for this event in Booking.
        if ("REFUNDED".equals(result)) {
            log.info("Ignoring refund lifecycle mutation for Booking {}", event.payload().bookingId());
            return;
        }

        BigDecimal amount = event.payload().amount() == null
                ? BigDecimal.ZERO : event.payload().amount();
        LocalDateTime occurredAt = event.occurredAt() == null
                ? null : LocalDateTime.ofInstant(event.occurredAt(), ZoneOffset.UTC);
        InternalPaymentResultRequest request = new InternalPaymentResultRequest(
                event.eventId(),
                event.schemaVersion(),
                event.payload().paymentId(),
                event.payload().transactionCode(),
                event.payload().paymentMethod(),
                result,
                amount,
                event.payload().currency(),
                occurredAt,
                event.payload().externalTransactionId(),
                null);
        paymentService.recordPaymentResult(event.payload().bookingId(), request);
        log.debug("Delegated payment event {} for Booking {}", event.eventId(), event.payload().bookingId());
    }

    private String normalizeResult(String eventType, String paymentStatus) {
        String candidate = eventType == null ? "" : eventType.trim().toUpperCase();
        return switch (candidate) {
            case "PAYMENT_SUCCESS" -> "SUCCESS";
            case "PAYMENT_FAILED" -> "FAILED";
            case "PAYMENT_CANCELLED" -> "CANCELLED";
            case "PAYMENT_EXPIRED", "PAYMENT_TIMEOUT" -> "TIMEOUT";
            case "PAYMENT_PENDING", "PAYMENT_REQUESTED", "PAYMENT_CREATED" -> "PENDING";
            case "PAYMENT_REFUNDED", "REFUND_SUCCESS" -> "REFUNDED";
            default -> paymentStatus == null ? "PENDING" : paymentStatus.trim().toUpperCase();
        };
    }
}
