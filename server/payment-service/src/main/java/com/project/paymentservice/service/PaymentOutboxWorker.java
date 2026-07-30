package com.project.paymentservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.paymentservice.client.booking.BookingPaymentClient;
import com.project.paymentservice.client.booking.BookingPaymentResultRequest;
import com.project.paymentservice.client.booking.BookingPaymentResultResponse;
import com.project.paymentservice.config.PaymentRuntimeProperties;
import com.project.paymentservice.entity.PaymentOutboxEvent;
import com.project.paymentservice.enumtype.OutboxDestination;
import com.project.paymentservice.exception.BusinessException;
import com.project.paymentservice.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class PaymentOutboxWorker {
    private static final Logger log = LoggerFactory.getLogger(PaymentOutboxWorker.class);

    private final OutboxDeliveryStateService stateService;
    private final BookingPaymentClient bookingClient;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final PaymentRuntimeProperties properties;
    private final ObjectMapper objectMapper;
    private final PaymentRepository paymentRepository;

    public PaymentOutboxWorker(
            OutboxDeliveryStateService stateService,
            BookingPaymentClient bookingClient,
            KafkaTemplate<String, String> kafkaTemplate,
            PaymentRuntimeProperties properties,
            ObjectMapper objectMapper,
            PaymentRepository paymentRepository) {
        this.stateService = stateService;
        this.bookingClient = bookingClient;
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.paymentRepository = paymentRepository;
    }

    @Scheduled(
            fixedDelayString = "${payment.runtime.outbox-fixed-delay-millis:2000}",
            initialDelayString = "${payment.runtime.outbox-initial-delay-millis:2000}")
    public void deliver() {
        String ownerToken = "payment-outbox-" + UUID.randomUUID();
        List<PaymentOutboxEvent> events;
        try {
            events = stateService.claim(ownerToken);
        } catch (Exception exception) {
            log.warn("Cannot claim Payment outbox: {}", exception.getMessage());
            return;
        }
        for (PaymentOutboxEvent event : events) {
            try {
                if (event.getDestination() == OutboxDestination.BOOKING_SERVICE_REST) {
                    deliverBooking(event, ownerToken);
                } else if (event.getDestination() == OutboxDestination.ANALYTICS_KAFKA) {
                    kafkaTemplate.send(
                                    properties.getAnalyticsTopic(),
                                    event.getAggregateId(),
                                    event.getPayload())
                            .get(10, TimeUnit.SECONDS);
                    stateService.markPublished(event.getId(), ownerToken);
                }
            } catch (Exception exception) {
                stateService.markFailed(event.getId(), ownerToken, rootMessage(exception));
            }
        }
    }

    private void deliverBooking(PaymentOutboxEvent event, String ownerToken) throws Exception {
        BookingPaymentResultRequest request = objectMapper.readValue(
                event.getPayload(), BookingPaymentResultRequest.class);
        try {
            BookingPaymentResultResponse response = "REFUND_RESULT".equals(event.getEventType())
                    ? bookingClient.notifyRefundResult(bookingPublicId(event), request)
                    : bookingClient.notifyPaymentResult(bookingPublicId(event), request);
            boolean accepted = Boolean.TRUE.equals(response.getAccepted())
                    && !Boolean.TRUE.equals(response.getReconciliationRequired());
            stateService.markBookingPublished(
                    event.getId(),
                    ownerToken,
                    accepted,
                    accepted ? null : "BOOKING_RECONCILIATION_REQUIRED");
        } catch (BusinessException exception) {
            if (exception.getHttpStatus() == org.springframework.http.HttpStatus.CONFLICT) {
                stateService.markBookingPublished(
                        event.getId(), ownerToken, false, exception.getErrorCode());
                return;
            }
            throw exception;
        }
    }

    private String bookingPublicId(PaymentOutboxEvent event) {
        return paymentRepository.findByPublicId(event.getAggregateId())
                .orElseThrow(() -> new IllegalStateException("Payment aggregate missing"))
                .getBookingPublicId();
    }

    private String rootMessage(Exception exception) {
        Throwable current = exception;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
