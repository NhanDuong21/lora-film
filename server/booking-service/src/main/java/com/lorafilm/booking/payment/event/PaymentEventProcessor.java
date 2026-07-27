package com.lorafilm.booking.payment.event;

import com.lorafilm.booking.booking.dto.request.InternalPaymentResultRequest;
import com.lorafilm.booking.booking.service.InternalBookingPaymentService;
import com.lorafilm.booking.payment.event.contract.PaymentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

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
        if (event == null || event.payload() == null
                || (event.payload().bookingPublicId() == null
                && event.payload().bookingId() == null)) {
            log.warn("Ignoring payment event without a Booking id: {}", event == null ? null : event.eventId());
            return;
        }

        String result = normalizeResult(event.eventType(), event.payload().paymentStatus());
        BigDecimal amount = event.payload().amount() == null
                ? BigDecimal.ZERO : event.payload().amount();
        InternalPaymentResultRequest request = new InternalPaymentResultRequest(
                event.eventId(),
                event.schemaVersion() == null ? "1.0" : event.schemaVersion(),
                event.payload().paymentId(),
                event.payload().paymentPublicId(),
                event.payload().transactionCode(),
                event.payload().paymentProvider(),
                event.payload().paymentMethod(),
                result,
                amount,
                event.payload().currency(),
                event.occurredAt(),
                event.payload().externalTransactionId());

        boolean refundResult = "REFUND_SUCCESS".equals(result) || "REFUND_FAILED".equals(result);
        if (event.payload().bookingPublicId() != null) {
            if (refundResult) {
                paymentService.recordRefundResult(event.payload().bookingPublicId(), request);
            } else {
                paymentService.recordPaymentResult(event.payload().bookingPublicId(), request);
            }
        } else {
            paymentService.recordPaymentResult(event.payload().bookingId(), request);
        }
        log.debug(
                "Delegated payment event {} for Booking {}",
                event.eventId(),
                event.payload().bookingPublicId() == null
                        ? event.payload().bookingId()
                        : event.payload().bookingPublicId());
    }

    private String normalizeResult(String eventType, String paymentStatus) {
        String candidate = eventType == null ? "" : eventType.trim().toUpperCase();
        return switch (candidate) {
            case "PAYMENT_SUCCESS" -> "SUCCESS";
            case "PAYMENT_FAILED" -> "FAILED";
            case "PAYMENT_CANCELLED" -> "CANCELLED";
            case "PAYMENT_EXPIRED", "PAYMENT_TIMEOUT" -> "TIMEOUT";
            case "PAYMENT_PENDING", "PAYMENT_REQUESTED", "PAYMENT_CREATED" -> "PENDING";
            case "PAYMENT_REFUNDED", "REFUND_SUCCESS" -> "REFUND_SUCCESS";
            case "REFUND_FAILED" -> "REFUND_FAILED";
            default -> paymentStatus == null ? "PENDING" : paymentStatus.trim().toUpperCase();
        };
    }
}
