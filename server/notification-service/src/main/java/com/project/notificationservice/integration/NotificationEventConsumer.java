package com.project.notificationservice.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.project.notificationservice.domain.NotificationTypes.Category;
import com.project.notificationservice.domain.NotificationTypes.Channel;
import com.project.notificationservice.domain.NotificationTypes.Priority;
import com.project.notificationservice.entity.NotificationEventInbox;
import com.project.notificationservice.exception.NotificationException;
import com.project.notificationservice.repository.NotificationEventInboxRepository;
import com.project.notificationservice.service.NotificationApplicationService;
import com.project.notificationservice.service.NotificationCommands.CreateNotificationCommand;
import com.project.notificationservice.service.NotificationCommands.RecipientCommand;
import com.project.notificationservice.service.NotificationCommands.CouponIssuedNotification;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "notification.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationEventConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationEventInboxRepository inboxRepository;
    private final NotificationApplicationService notificationService;
    private final UserRecipientClient userRecipientClient;

    public NotificationEventConsumer(
            ObjectMapper objectMapper,
            NotificationEventInboxRepository inboxRepository,
            NotificationApplicationService notificationService,
            UserRecipientClient userRecipientClient) {
        this.objectMapper = objectMapper;
        this.inboxRepository = inboxRepository;
        this.notificationService = notificationService;
        this.userRecipientClient = userRecipientClient;
    }

    @KafkaListener(topics = "${notification.kafka.booking-topic:booking.domain.events.v1}")
    @Transactional
    public void consumeBookingEvent(String json) {
        DomainEventEnvelope event = parse(json);
        if (!acceptInbox(event, json)) return;
        String type = normalize(event.eventType());
        if (!"TICKET_ISSUED".equals(type)) {
            markProcessed(event);
            return;
        }
        Map<String, Object> payload = event.payload() == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(event.payload());
        String email = text(payload, "email");
        String phone = text(payload, "phone");
        String push = text(payload, "webPushSubscription");
        String userPublicId = event.userPublicId() == null
                ? text(payload, "userPublicId") : event.userPublicId();
        if (email == null && userPublicId != null) {
            try {
                UserRecipientClient.ResolvedRecipient resolved = userRecipientClient
                        .findByUserPublicId(userPublicId)
                        .orElse(null);
                if (resolved != null) {
                    email = resolved.email();
                    if (resolved.fullName() != null) {
                        payload.put("customerName", resolved.fullName());
                    }
                }
            } catch (Exception ex) {
                org.slf4j.LoggerFactory.getLogger(NotificationEventConsumer.class)
                        .warn("Could not resolve recipient email for userPublicId={}: {}", userPublicId, ex.getMessage());
            }
        }
        Set<Channel> channels = new LinkedHashSet<>();
        if (email != null) channels.add(Channel.EMAIL);
        if (userPublicId != null) channels.add(Channel.IN_APP);
        if (push != null) channels.add(Channel.WEB_PUSH);
        if (channels.isEmpty()) {
            throw new NotificationException("TICKET_EVENT_RECIPIENT_MISSING",
                    "ticket.issued event has no deliverable recipient", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        payload.remove("email");
        payload.remove("phone");
        payload.remove("userPublicId");
        payload.remove("webPushSubscription");
        payload.remove("locale");
        notificationService.accept(new CreateNotificationCommand(
                "ticket-issued-" + event.eventId(),
                event.source() == null ? "booking-service" : event.source(),
                event.eventId(),
                "TICKET_ISSUED",
                event.correlationId(),
                event.causationId(),
                "BOOKING_CONFIRMED",
                event.locale() == null ? "vi-VN" : event.locale(),
                Category.TRANSACTIONAL,
                Priority.HIGH,
                null,
                event.occurredAt() == null ? null : event.occurredAt().plusSeconds(7 * 24 * 3600),
                false,
                new RecipientCommand(userPublicId, email, phone, push),
                channels,
                payload));
        markProcessed(event);
    }

    @KafkaListener(topics = "${notification.kafka.payment-topic:payment.domain.events.v1}")
    @Transactional
    public void consumePaymentEvent(String json) {
        DomainEventEnvelope event = parse(json);
        if (!acceptInbox(event, json)) return;
        // A payment success is intentionally not enough to create TICKET_PURCHASED.
        markProcessed(event);
    }

    @KafkaListener(topics = "${notification.kafka.promotion-topic:promotion.lifecycle}")
    @Transactional
    public void consumePromotionEvent(String json) {
        JsonNode event = parsePromotionEnvelope(json);
        String eventId = text(event, "eventId");
        String eventType = normalize(text(event, "eventType"));
        if (!acceptPromotionInbox(eventId, eventType, json)) return;
        if (!"COUPON_ISSUED".equals(eventType)) {
            markPromotionProcessed(eventId);
            return;
        }
        JsonNode data = event.path("data");
        String userPublicId = text(data, "userPublicId");
        String couponCode = text(data, "couponCode");
        String promotionName = text(data, "promotionName");
        if (userPublicId == null || couponCode == null || promotionName == null) {
            throw new NotificationException("EVENT_CONTRACT_INVALID",
                    "coupon.issued event is missing recipient or coupon details",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        notificationService.acceptCouponIssued(new CouponIssuedNotification(
                eventId, userPublicId, couponCode, promotionName,
                instant(text(data, "validTo")), defaultText(text(data, "deepLink"), "/booking")));
        markPromotionProcessed(eventId);
    }

    private DomainEventEnvelope parse(String json) {
        try {
            DomainEventEnvelope event = objectMapper.readValue(json, DomainEventEnvelope.class);
            if (event.eventId() == null || event.eventType() == null || event.eventVersion() < 1) {
                throw new NotificationException("EVENT_CONTRACT_INVALID",
                        "Event id, type, and a positive version are required",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
            return event;
        } catch (JsonProcessingException exception) {
            throw new NotificationException("EVENT_CONTRACT_INVALID",
                    "Kafka event is not valid JSON", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private JsonNode parsePromotionEnvelope(String json) {
        try {
            JsonNode event = objectMapper.readTree(json);
            if (text(event, "eventId") == null || text(event, "eventType") == null) {
                throw new NotificationException("EVENT_CONTRACT_INVALID",
                        "Promotion event id and type are required",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
            return event;
        } catch (JsonProcessingException exception) {
            throw new NotificationException("EVENT_CONTRACT_INVALID",
                    "Promotion Kafka event is not valid JSON", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private boolean acceptInbox(DomainEventEnvelope event, String json) {
        String source = event.source() == null ? "unknown" : event.source();
        if (inboxRepository.existsBySourceServiceAndSourceEventId(source, event.eventId())) {
            return false;
        }
        NotificationEventInbox inbox = new NotificationEventInbox();
        inbox.setSourceService(source);
        inbox.setSourceEventId(event.eventId());
        inbox.setEventType(event.eventType());
        inbox.setEventVersion(event.eventVersion());
        inbox.setPayloadJson(json);
        inboxRepository.saveAndFlush(inbox);
        return true;
    }

    private void markProcessed(DomainEventEnvelope event) {
        inboxRepository.findBySourceServiceAndSourceEventId(
                        event.source() == null ? "unknown" : event.source(), event.eventId())
                .ifPresent(item -> {
                    item.setStatus("PROCESSED");
                    item.setProcessedAt(Instant.now());
                });
    }

    private boolean acceptPromotionInbox(String eventId, String eventType, String json) {
        if (inboxRepository.existsBySourceServiceAndSourceEventId("promotion-service", eventId)) {
            return false;
        }
        NotificationEventInbox inbox = new NotificationEventInbox();
        inbox.setSourceService("promotion-service");
        inbox.setSourceEventId(eventId);
        inbox.setEventType(eventType);
        inbox.setEventVersion(1);
        inbox.setPayloadJson(json);
        inboxRepository.saveAndFlush(inbox);
        return true;
    }

    private void markPromotionProcessed(String eventId) {
        inboxRepository.findBySourceServiceAndSourceEventId("promotion-service", eventId)
                .ifPresent(item -> {
                    item.setStatus("PROCESSED");
                    item.setProcessedAt(Instant.now());
                });
    }

    private String normalize(String type) {
        return type.trim().toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_');
    }

    private String text(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value == null || String.valueOf(value).isBlank() ? null : String.valueOf(value);
    }

    private String text(JsonNode node, String key) {
        JsonNode value = node == null ? null : node.get(key);
        return value == null || value.isNull() || value.asText().isBlank()
                ? null : value.asText();
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private Instant instant(String value) {
        if (value == null) return null;
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException exception) {
            throw new NotificationException("EVENT_CONTRACT_INVALID",
                    "coupon.issued validTo must be an ISO-8601 timestamp",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }
}
