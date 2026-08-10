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
import com.project.notificationservice.service.NotificationCommands.VoucherGrantedNotification;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "notification.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationEventConsumer {

    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi-VN");
    private static final DateTimeFormatter VIETNAMESE_DATE_TIME = DateTimeFormatter
            .ofPattern("dd/MM/yyyy HH:mm")
            .withLocale(VIETNAMESE)
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private final ObjectMapper objectMapper;
    private final NotificationEventInboxRepository inboxRepository;
    private final NotificationApplicationService notificationService;
    private final UserRecipientClient userRecipientClient;
    private final String frontendBaseUrl;

    public NotificationEventConsumer(
            ObjectMapper objectMapper,
            NotificationEventInboxRepository inboxRepository,
            NotificationApplicationService notificationService,
            UserRecipientClient userRecipientClient,
            @Value("${notification.frontend-base-url:${FRONTEND_URL:http://localhost:5173}}")
            String frontendBaseUrl) {
        this.objectMapper = objectMapper;
        this.inboxRepository = inboxRepository;
        this.notificationService = notificationService;
        this.userRecipientClient = userRecipientClient;
        String resolvedFrontendBaseUrl = frontendBaseUrl == null || frontendBaseUrl.isBlank()
                ? "http://localhost:5173"
                : frontendBaseUrl.strip();
        this.frontendBaseUrl = resolvedFrontendBaseUrl.replaceAll("/+$", "");
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
        if (!Set.of("VOUCHER_GRANTED", "COUPON_ISSUED").contains(eventType)) {
            markPromotionProcessed(eventId);
            return;
        }
        JsonNode data = event.path("data");
        String userPublicId = text(data, "userPublicId");
        String voucherCode = firstText(data, "voucherCode", "couponCode", "promotionCode");
        String voucherName = firstText(data, "voucherName", "promotionName");
        if (userPublicId == null || voucherCode == null || voucherName == null) {
            throw new NotificationException("EVENT_CONTRACT_INVALID",
                    "voucher.granted event is missing recipient or voucher details",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        UserRecipientClient.ResolvedRecipient recipient = userRecipientClient
                .findByUserPublicId(userPublicId)
                .orElseThrow(() -> new NotificationException(
                        "VOUCHER_RECIPIENT_NOT_FOUND",
                        "Voucher recipient could not be resolved",
                        HttpStatus.UNPROCESSABLE_ENTITY));
        if (recipient.email() == null || recipient.email().isBlank()) {
            throw new NotificationException(
                    "VOUCHER_RECIPIENT_EMAIL_MISSING",
                    "Voucher recipient does not have an email address",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        Instant expiresAt = instant(text(data, "validTo"));
        String deepLink = safeDeepLink(text(data, "deepLink"));
        notificationService.acceptVoucherGranted(new VoucherGrantedNotification(
                eventId, userPublicId, recipient.email(),
                defaultText(recipient.fullName(), "Quý khách"),
                voucherCode, voucherName,
                discountLabel(data), minimumOrderLabel(data),
                expiresAt == null ? "Không giới hạn" : VIETNAMESE_DATE_TIME.format(expiresAt),
                expiresAt, deepLink, frontendBaseUrl + deepLink));
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

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (value != null) return value;
        }
        return null;
    }

    private BigDecimal decimal(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node == null ? null : node.get(field);
            if (value == null || value.isNull()) continue;
            try {
                return value.isNumber()
                        ? value.decimalValue()
                        : new BigDecimal(value.asText());
            } catch (NumberFormatException exception) {
                throw new NotificationException("EVENT_CONTRACT_INVALID",
                        field + " must be numeric", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }
        return null;
    }

    private String discountLabel(JsonNode data) {
        String type = defaultText(text(data, "discountType"), "").toUpperCase(Locale.ROOT);
        BigDecimal value = decimal(data, "discountValue");
        if (Set.of("PERCENTAGE", "PERCENT").contains(type) && value != null) {
            return value.stripTrailingZeros().toPlainString() + "%";
        }
        if (Set.of("FIXED_AMOUNT", "AMOUNT").contains(type) && value != null) {
            return formatCurrency(value);
        }
        if (Set.of("FREE", "FULL_DISCOUNT").contains(type)) {
            return "Miễn phí";
        }
        return value == null ? "Theo điều kiện chương trình" : value.toPlainString();
    }

    private String minimumOrderLabel(JsonNode data) {
        BigDecimal minimum = decimal(data, "minimumOrderAmount", "minOrderAmount");
        return minimum == null || minimum.signum() <= 0
                ? "Không yêu cầu"
                : formatCurrency(minimum);
    }

    private String formatCurrency(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(VIETNAMESE).format(value);
    }

    private String safeDeepLink(String value) {
        String path = defaultText(value, "/booking").strip();
        return path.startsWith("/") && !path.startsWith("//") ? path : "/booking";
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
