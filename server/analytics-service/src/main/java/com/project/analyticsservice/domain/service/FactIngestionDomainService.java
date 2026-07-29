package com.project.analyticsservice.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.analyticsservice.entity.*;
import com.project.analyticsservice.exception.NonRetryableAnalyticsEventException;
import com.project.analyticsservice.kafka.event.BookingCancelledEvent;
import com.project.analyticsservice.kafka.event.PaymentRefundedEvent;
import com.project.analyticsservice.kafka.event.PaymentSucceededEvent;
import com.project.analyticsservice.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.ZoneId;

@Service
public class FactIngestionDomainService {
    private final ObjectMapper objectMapper;
    private final EventValidationService validationService;
    private final PayloadHashService payloadHashService;
    private final ProcessedAnalyticsEventRepository processedEventRepository;
    private final FactBookingMetricRepository bookingFactRepository;
    private final FactBookingCancellationRepository cancellationFactRepository;
    private final FactPaymentRefundRepository refundFactRepository;
    private final ZoneId businessZone;

    public FactIngestionDomainService(
            ObjectMapper objectMapper,
            EventValidationService validationService,
            PayloadHashService payloadHashService,
            ProcessedAnalyticsEventRepository processedEventRepository,
            FactBookingMetricRepository bookingFactRepository,
            FactBookingCancellationRepository cancellationFactRepository,
            FactPaymentRefundRepository refundFactRepository,
            @Value("${analytics.zone-id:Asia/Ho_Chi_Minh}") String zoneId) {
        this.objectMapper = objectMapper;
        this.validationService = validationService;
        this.payloadHashService = payloadHashService;
        this.processedEventRepository = processedEventRepository;
        this.bookingFactRepository = bookingFactRepository;
        this.cancellationFactRepository = cancellationFactRepository;
        this.refundFactRepository = refundFactRepository;
        this.businessZone = ZoneId.of(zoneId);
    }

    @Transactional
    public void ingestPaymentSucceeded(String payload) {
        ingestPaymentSucceeded(payload, EventSourceMetadata.unknown());
    }

    @Transactional
    public void ingestPaymentSucceeded(String payload, EventSourceMetadata metadata) {
        PaymentSucceededEvent event = read(payload, PaymentSucceededEvent.class);
        validationService.validate(event);
        if (alreadyProcessed(event.eventId(), payload)) {
            return;
        }

        persistProcessed(event.eventId(), "PAYMENT_SUCCEEDED", "payment-service",
                event.bookingPublicId(), event.schemaVersion(), payload, metadata, event.succeededAt());

        FactBookingMetric fact = new FactBookingMetric();
        fact.setEventId(event.eventId());
        fact.setPaymentPublicId(event.paymentPublicId());
        fact.setBookingPublicId(event.bookingPublicId());
        fact.setUserPublicId(trimToNull(event.userPublicId()));
        fact.setMovieId(event.movieId());
        fact.setMoviePublicId(trimToNull(event.moviePublicId()));
        fact.setMovieKey(event.movieId() != null
                ? String.valueOf(event.movieId())
                : requiredFallback(event.moviePublicId(), "moviePublicId"));
        fact.setMovieTitle(event.movieTitle().trim());
        fact.setCinemaPublicId(trimToNull(event.cinemaPublicId()));
        fact.setCinemaName(trimToNull(event.cinemaName()));
        fact.setAuditoriumPublicId(trimToNull(event.auditoriumPublicId()));
        fact.setShowtimePublicId(trimToNull(event.showtimePublicId()));
        fact.setPromotionPublicId(trimToNull(event.promotionPublicId()));
        fact.setPromotionName(trimToNull(event.promotionName()));
        fact.setMembershipTier(normalizeTier(event.membershipTier()));
        fact.setPaymentMethod(StringUtils.hasText(event.paymentMethod())
                ? event.paymentMethod().trim().toUpperCase()
                : event.provider().trim().toUpperCase());
        fact.setCurrency(event.currency());
        fact.setGrossAmount(event.ticketAmount().add(event.foodAmount()));
        fact.setDiscountAmount(event.discountAmount());
        fact.setNetRevenue(event.totalAmount());
        fact.setTicketCount(event.ticketCount());
        fact.setAvailableSeats(event.availableSeats());
        fact.setBookingStatus("SUCCESS");
        fact.setOccurredAt(event.succeededAt());
        fact.setBusinessDate(event.succeededAt().atZone(businessZone).toLocalDate());
        bookingFactRepository.save(fact);
    }

    @Transactional
    public void ingestBookingCancelled(String payload) {
        ingestBookingCancelled(payload, EventSourceMetadata.unknown());
    }

    @Transactional
    public void ingestBookingCancelled(String payload, EventSourceMetadata metadata) {
        BookingCancelledEvent event = read(payload, BookingCancelledEvent.class);
        validationService.validate(event);
        if (alreadyProcessed(event.eventId(), payload)) {
            return;
        }
        String bookingKey = StringUtils.hasText(event.bookingPublicId())
                ? event.bookingPublicId().trim()
                : String.valueOf(event.bookingId());
        persistProcessed(event.eventId(), "BOOKING_CANCELLED", "booking-service",
                bookingKey, event.eventVersion(), payload, metadata, event.occurredAt());

        FactBookingCancellation fact = new FactBookingCancellation();
        fact.setEventId(event.eventId());
        fact.setBookingKey(bookingKey);
        fact.setPreviousStatus(event.previousStatus().trim().toUpperCase());
        fact.setReason(event.reason());
        fact.setOccurredAt(event.occurredAt());
        fact.setBusinessDate(event.occurredAt().atZone(businessZone).toLocalDate());
        cancellationFactRepository.save(fact);
    }

    @Transactional
    public void ingestPaymentRefunded(String payload) {
        ingestPaymentRefunded(payload, EventSourceMetadata.unknown());
    }

    @Transactional
    public void ingestPaymentRefunded(String payload, EventSourceMetadata metadata) {
        PaymentRefundedEvent event = read(payload, PaymentRefundedEvent.class);
        validationService.validate(event);
        if (alreadyProcessed(event.eventId(), payload)) {
            return;
        }
        persistProcessed(event.eventId(), "PAYMENT_REFUNDED", "payment-service",
                event.bookingPublicId(), event.schemaVersion(), payload, metadata, event.refundedAt());

        FactPaymentRefund fact = new FactPaymentRefund();
        fact.setEventId(event.eventId());
        fact.setPaymentPublicId(event.paymentPublicId());
        fact.setBookingPublicId(event.bookingPublicId());
        fact.setRefundAmount(event.refundAmount());
        fact.setCurrency(event.currency());
        fact.setOccurredAt(event.refundedAt());
        fact.setRefundDate(event.refundedAt().atZone(businessZone).toLocalDate());
        refundFactRepository.save(fact);
    }

    private void persistProcessed(
            String eventId, String eventType, String sourceService,
            String aggregateKey, String schemaVersion, String payload,
            EventSourceMetadata metadata, java.time.Instant eventOccurredAt) {
        ProcessedAnalyticsEvent processed = new ProcessedAnalyticsEvent();
        processed.setEventId(eventId);
        processed.setEventType(eventType);
        processed.setSourceService(sourceService);
        processed.setAggregateKey(aggregateKey);
        processed.setSchemaVersion(schemaVersion);
        processed.setPayloadHash(payloadHashService.sha256(payload));
        processed.setSourceTopic(metadata.topic());
        processed.setSourcePartition(metadata.partition());
        processed.setSourceOffset(metadata.offset());
        processed.setCorrelationId(metadata.correlationId());
        processed.setTraceId(metadata.traceId());
        processed.setEventOccurredAt(eventOccurredAt);
        processed.setReceivedAt(metadata.receivedAt());
        processedEventRepository.saveAndFlush(processed);
    }

    private boolean alreadyProcessed(String eventId, String payload) {
        return processedEventRepository.findByEventId(eventId)
                .map(processed -> {
                    String incomingHash = payloadHashService.sha256(payload);
                    if (!incomingHash.equals(processed.getPayloadHash())) {
                        throw new NonRetryableAnalyticsEventException(
                                "eventId was reused with a different payload");
                    }
                    return true;
                })
                .orElse(false);
    }

    private <T> T read(String payload, Class<T> type) {
        try {
            return objectMapper.readValue(payload, type);
        } catch (JsonProcessingException exception) {
            throw new NonRetryableAnalyticsEventException("Malformed analytics event payload", exception);
        }
    }

    private String requiredFallback(String value, String name) {
        if (!StringUtils.hasText(value)) {
            throw new NonRetryableAnalyticsEventException(name + " is required when movieId is absent");
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeTier(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : "UNKNOWN";
    }
}
