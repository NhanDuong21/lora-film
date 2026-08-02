package com.lorafilm.booking.infrastructure.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.infrastructure.entity.BookingInboxEvent;
import com.lorafilm.booking.infrastructure.repository.BookingInboxEventRepository;
import com.lorafilm.booking.infrastructure.service.PromotionConfirmationReconciliationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@ConditionalOnProperty(
        name = "booking.kafka.promotion-consumer-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class PromotionLifecycleEventConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(PromotionLifecycleEventConsumer.class);

    private final BookingInboxEventRepository inboxRepository;
    private final PromotionConfirmationReconciliationService reconciliationService;
    private final ObjectMapper objectMapper;

    public PromotionLifecycleEventConsumer(
            BookingInboxEventRepository inboxRepository,
            PromotionConfirmationReconciliationService reconciliationService,
            ObjectMapper objectMapper) {
        this.inboxRepository = inboxRepository;
        this.reconciliationService = reconciliationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${booking.kafka.promotion-lifecycle-topic:promotion.lifecycle}",
            groupId = "${booking.kafka.promotion-group-id:booking-promotion-reconciliation-v1}")
    @Transactional
    public void consume(String eventJson) {
        JsonNode event = parse(eventJson);
        String eventType = text(event, "eventType");
        if (!"RESERVATION_CONFIRMED".equals(eventType)
                && !"RESERVATION_REVERSED".equals(eventType)) {
            return;
        }
        JsonNode data = event.path("data");
        String bookingPublicId = text(data, "bookingPublicId");
        if (bookingPublicId == null) {
            log.debug("Ignoring promotion lifecycle event without bookingPublicId");
            return;
        }
        String eventId = requiredText(event, "eventId");
        BookingInboxEvent inbox = inboxRepository.findByEventId(eventId).orElse(null);
        if (inbox != null && Boolean.TRUE.equals(inbox.getProcessed())) {
            return;
        }
        if (inbox == null) {
            inbox = new BookingInboxEvent();
            inbox.setEventId(eventId);
            inbox.setSourceService("promotion-service");
            inbox.setAggregateType(text(event, "aggregateType"));
            inbox.setEventType(eventType);
            inbox.setPayload(eventJson);
            inbox.setProcessed(false);
            inbox = inboxRepository.save(inbox);
        }

        reconciliationService.observeLifecycleEvent(
                requiredText(data, "publicId"),
                bookingPublicId,
                text(data, "paymentPublicId"),
                eventType);
        inbox.setProcessed(true);
        inbox.setProcessedAt(Instant.now());
        inbox.setErrorMessage(null);
        inboxRepository.save(inbox);
        log.info("Observed promotion lifecycle event {} for booking {}",
                eventId, bookingPublicId);
    }

    private JsonNode parse(String eventJson) {
        try {
            return objectMapper.readTree(eventJson);
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Promotion lifecycle event is not valid JSON", exception);
        }
    }

    private String requiredText(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null) {
            throw new IllegalArgumentException(
                    "Promotion lifecycle event is missing " + field);
        }
        return value;
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.path(field).isNull()) {
            return null;
        }
        String value = node.path(field).asText().trim();
        return value.isEmpty() ? null : value;
    }
}
