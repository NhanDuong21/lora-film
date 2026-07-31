package com.project.scoreservice.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.scoreservice.dto.ScoreEarnRequest;
import com.project.scoreservice.dto.ScoreEarnResponse;
import com.project.scoreservice.service.ScoreService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

/**
 * Awards loyalty points from Booking's authoritative confirmation event.
 *
 * <p>The Booking outbox uses the booking public ID as the Kafka key and the
 * outbox event ID as a header. Score's database idempotency key remains based
 * on the numeric booking ID so replaying the same business transition cannot
 * award points twice, even if an upstream replay uses a new Kafka offset.</p>
 */
@Component
@ConditionalOnProperty(
        name = "score.kafka.booking-events-consumer.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class BookingConfirmedScoreConsumer {
    private static final Logger log =
            LoggerFactory.getLogger(BookingConfirmedScoreConsumer.class);

    private final ObjectMapper objectMapper;
    private final ScoreService scoreService;

    public BookingConfirmedScoreConsumer(
            ObjectMapper objectMapper,
            ScoreService scoreService) {
        this.objectMapper = objectMapper;
        this.scoreService = scoreService;
    }

    @KafkaListener(
            topics = "${score.kafka.booking-events-topic:booking.events.v1}",
            groupId = "${score.kafka.booking-events-consumer.group-id:"
                    + "score-service-booking-confirmed-v1}",
            concurrency = "${score.kafka.booking-events-consumer.concurrency:1}",
            autoStartup = "${score.kafka.booking-events-consumer.enabled:true}")
    public void onBookingEvent(ConsumerRecord<String, String> record) {
        JsonNode event = parse(record.value());
        if (!"CONFIRMED".equalsIgnoreCase(text(event, "bookingStatus"))
                || !"SUCCESS".equalsIgnoreCase(text(event, "paymentStatus"))) {
            return;
        }

        long bookingId = positiveLong(event, "id");
        long userId = positiveLong(event, "userId");
        BigDecimal eligibleAmount = positiveAmount(event, "finalAmount");
        if (eligibleAmount.signum() == 0) {
            log.info("Confirmed booking has no cash amount eligible for points: bookingId={}",
                    bookingId);
            return;
        }

        String publicId = text(event, "publicId");
        String eventId = header(record, "event-id");
        if (eventId == null || eventId.isBlank()) {
            eventId = "BOOKING_CONFIRMED:"
                    + (publicId == null || publicId.isBlank() ? bookingId : publicId);
        }

        ScoreEarnResponse result = scoreService.earnPoints(new ScoreEarnRequest(
                userId,
                bookingId,
                eligibleAmount,
                eventId,
                "EARN:BOOKING:" + bookingId));
        log.info(
                "Processed confirmed booking score event: bookingId={}, userId={}, "
                        + "earnedPoints={}, idempotent={}",
                bookingId, userId, result.pointChange(), result.idempotent());
    }

    private JsonNode parse(String payload) {
        try {
            JsonNode event = objectMapper.readTree(payload);
            if (event == null || !event.isObject()) {
                throw new IllegalArgumentException("Booking event payload must be a JSON object");
            }
            return event;
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid Booking event JSON", exception);
        }
    }

    private long positiveLong(JsonNode event, String field) {
        JsonNode value = event.get(field);
        if (value == null || !value.canConvertToLong() || value.asLong() <= 0) {
            throw new IllegalArgumentException(
                    "Confirmed Booking event requires a positive " + field);
        }
        return value.asLong();
    }

    private BigDecimal positiveAmount(JsonNode event, String field) {
        JsonNode value = event.get(field);
        if (value == null || !value.isNumber()) {
            throw new IllegalArgumentException(
                    "Confirmed Booking event requires numeric " + field);
        }
        BigDecimal amount = value.decimalValue();
        if (amount.signum() < 0) {
            throw new IllegalArgumentException(
                    "Confirmed Booking event requires non-negative " + field);
        }
        return amount;
    }

    private String text(JsonNode event, String field) {
        JsonNode value = event.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String header(ConsumerRecord<String, String> record, String name) {
        Header value = record.headers().lastHeader(name);
        return value == null
                ? null : new String(value.value(), StandardCharsets.UTF_8);
    }
}
