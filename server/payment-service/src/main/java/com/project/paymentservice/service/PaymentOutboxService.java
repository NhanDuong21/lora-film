package com.project.paymentservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.paymentservice.client.booking.BookingPaymentResultRequest;
import com.project.paymentservice.entity.Payment;
import com.project.paymentservice.entity.PaymentAnalyticsSnapshot;
import com.project.paymentservice.entity.PaymentOutboxEvent;
import com.project.paymentservice.entity.PaymentRefund;
import com.project.paymentservice.enumtype.OutboxDestination;
import com.project.paymentservice.enumtype.OutboxStatus;
import com.project.paymentservice.repository.PaymentOutboxEventRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

@Service
public class PaymentOutboxService {
    private final PaymentOutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public PaymentOutboxService(
            PaymentOutboxEventRepository repository,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public PaymentOutboxEvent enqueueBookingResult(
            Payment payment, String result, Instant occurredAt) {
        BookingPaymentResultRequest request = new BookingPaymentResultRequest();
        String eventId = UUID.randomUUID().toString();
        request.setEventId(eventId);
        request.setSchemaVersion("1.0");
        request.setPaymentId(payment.getId());
        request.setPaymentPublicId(payment.getPublicId());
        request.setPaymentTransactionCode(payment.getPaymentTransactionCode());
        request.setPaymentProvider(payment.getProviderCode().name());
        request.setPaymentMethod(payment.getPaymentMethod().name());
        request.setResult(result);
        request.setAmount(payment.getAmount());
        request.setCurrency(payment.getCurrency());
        request.setOccurredAt(occurredAt);
        request.setExternalTransactionId(payment.getExternalTransactionId());

        PaymentOutboxEvent event = baseEvent(
                payment, eventId, "PAYMENT_RESULT", OutboxDestination.BOOKING_SERVICE_REST);
        event.setPayload(writeJson(request));
        return repository.save(event);
    }

    public PaymentOutboxEvent enqueueAnalyticsSuccess(
            Payment payment, PaymentAnalyticsSnapshot snapshot, String correlationId) {
        String eventId = UUID.nameUUIDFromBytes(
                ("analytics:" + correlationId).getBytes(StandardCharsets.UTF_8)).toString();
        PaymentOutboxEvent existing = repository.findByEventId(eventId).orElse(null);
        if (existing != null) {
            return existing;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", eventId);
        payload.put("schemaVersion", "1.0");
        payload.put("paymentPublicId", payment.getPublicId());
        payload.put("bookingPublicId", payment.getBookingPublicId());
        payload.put("provider", payment.getProviderCode().name());
        payload.put("amount", payment.getAmount());
        payload.put("currency", payment.getCurrency());
        payload.put("succeededAt", payment.getSucceededAt());
        payload.put("movieId", snapshot.getMovieId());
        payload.put("moviePublicId", snapshot.getMoviePublicId());
        payload.put("movieTitle", snapshot.getMovieTitle());
        payload.put("showtimePublicId", snapshot.getShowtimePublicId());
        payload.put("cinemaPublicId", snapshot.getCinemaPublicId());
        payload.put("ticketCount", snapshot.getTicketCount());
        payload.put("ticketAmount", snapshot.getTicketAmount());
        payload.put("foodAmount", snapshot.getFoodAmount());
        payload.put("discountAmount", snapshot.getDiscountAmount());
        payload.put("totalAmount", snapshot.getTotalAmount());

        PaymentOutboxEvent event = baseEvent(
                payment, eventId, "PAYMENT_SUCCEEDED", OutboxDestination.ANALYTICS_KAFKA);
        event.setCorrelationId(correlationId);
        event.setPayload(writeJson(payload));
        return repository.save(event);
    }

    public PaymentOutboxEvent enqueueBookingRefundResult(
            PaymentRefund refund, boolean success, Instant occurredAt) {
        Payment payment = refund.getPayment();
        BookingPaymentResultRequest request = new BookingPaymentResultRequest();
        String eventId = UUID.randomUUID().toString();
        request.setEventId(eventId);
        request.setSchemaVersion("1.0");
        request.setPaymentId(payment.getId());
        request.setPaymentPublicId(payment.getPublicId());
        request.setPaymentTransactionCode(payment.getPaymentTransactionCode());
        request.setPaymentProvider(payment.getProviderCode().name());
        request.setPaymentMethod(payment.getPaymentMethod().name());
        request.setResult(success ? "REFUND_SUCCESS" : "REFUND_FAILED");
        request.setAmount(refund.getRequestedAmount());
        request.setCurrency(refund.getCurrency());
        request.setOccurredAt(occurredAt);
        request.setExternalTransactionId(refund.getProviderRefundId());

        PaymentOutboxEvent event = baseEvent(
                payment, eventId, "REFUND_RESULT", OutboxDestination.BOOKING_SERVICE_REST);
        event.setCorrelationId(refund.getPublicId());
        event.setPayload(writeJson(request));
        return repository.save(event);
    }

    public PaymentOutboxEvent enqueueAnalyticsRefund(
            Payment payment,
            PaymentRefund refund,
            PaymentAnalyticsSnapshot snapshot,
            String correlationId) {
        String eventId = UUID.nameUUIDFromBytes(
                ("analytics-refund:" + correlationId).getBytes(StandardCharsets.UTF_8)).toString();
        PaymentOutboxEvent existing = repository.findByEventId(eventId).orElse(null);
        if (existing != null) {
            return existing;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", eventId);
        payload.put("schemaVersion", "1.0");
        payload.put("eventType", "PAYMENT_REFUNDED");
        payload.put("refundPublicId", refund.getPublicId());
        payload.put("paymentPublicId", payment.getPublicId());
        payload.put("bookingPublicId", payment.getBookingPublicId());
        payload.put("provider", refund.getProviderCode().name());
        payload.put("refundType", refund.getRefundType().name());
        payload.put("refundComponent", refund.getRefundComponent().name());
        payload.put("reasonCode", refund.getReasonCode());
        payload.put("amount", refund.getRequestedAmount());
        payload.put("currency", refund.getCurrency());
        payload.put("refundedAt", refund.getSucceededAt());
        payload.put("moviePublicId", snapshot.getMoviePublicId());
        payload.put("movieTitle", snapshot.getMovieTitle());
        payload.put("showtimePublicId", snapshot.getShowtimePublicId());
        payload.put("cinemaPublicId", snapshot.getCinemaPublicId());

        PaymentOutboxEvent event = baseEvent(
                payment, eventId, "PAYMENT_REFUNDED", OutboxDestination.ANALYTICS_KAFKA);
        event.setCorrelationId(correlationId);
        event.setPayload(writeJson(payload));
        return repository.save(event);
    }

    public String deliveryStatus(String paymentPublicId) {
        return repository.findByAggregateIdAndStatus(paymentPublicId, OutboxStatus.PUBLISHED).stream()
                .anyMatch(event -> event.getDestination() == OutboxDestination.BOOKING_SERVICE_REST)
                ? "DELIVERED" : "PENDING";
    }

    private PaymentOutboxEvent baseEvent(
            Payment payment,
            String eventId,
            String eventType,
            OutboxDestination destination) {
        PaymentOutboxEvent event = new PaymentOutboxEvent();
        event.setEventId(eventId);
        event.setAggregateType("PAYMENT");
        event.setAggregateId(payment.getPublicId());
        event.setEventType(eventType);
        event.setSchemaVersion("1.0");
        event.setDestination(destination);
        event.setStatus(OutboxStatus.PENDING);
        event.setAttemptCount(0);
        return event;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize Payment outbox payload", exception);
        }
    }
}
