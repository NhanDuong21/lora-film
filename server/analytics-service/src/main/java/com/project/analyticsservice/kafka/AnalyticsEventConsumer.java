package com.project.analyticsservice.kafka;

import com.project.analyticsservice.application.EventIngestionApplicationService;
import com.project.analyticsservice.domain.service.EventSourceMetadata;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
public class AnalyticsEventConsumer {
    private final EventIngestionApplicationService ingestionService;

    public AnalyticsEventConsumer(EventIngestionApplicationService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @KafkaListener(
            topics = "${analytics.kafka.payment-success-topic:payment-success.v1}",
            groupId = "${spring.kafka.consumer.group-id:analytics-service-v1}",
            autoStartup = "${analytics.kafka.listener-enabled:true}")
    public void onPaymentSucceeded(ConsumerRecord<String, String> record) {
        ingestionService.ingestPaymentSucceeded(record.value(), metadata(record));
    }

    @KafkaListener(
            topics = "${analytics.kafka.payment-refund-topic:payment-refunded.v1}",
            groupId = "${spring.kafka.consumer.group-id:analytics-service-v1}",
            autoStartup = "${analytics.kafka.listener-enabled:true}")
    public void onPaymentRefunded(ConsumerRecord<String, String> record) {
        ingestionService.ingestPaymentRefunded(record.value(), metadata(record));
    }

    @KafkaListener(
            topics = "${analytics.kafka.booking-cancelled-topic:booking.booking-cancelled.v1}",
            groupId = "${spring.kafka.consumer.group-id:analytics-service-v1}",
            autoStartup = "${analytics.kafka.listener-enabled:true}")
    public void onBookingCancelled(ConsumerRecord<String, String> record) {
        ingestionService.ingestBookingCancelled(record.value(), metadata(record));
    }

    private EventSourceMetadata metadata(ConsumerRecord<String, String> record) {
        return new EventSourceMetadata(
                record.topic(),
                record.partition(),
                record.offset(),
                header(record, "correlation-id"),
                header(record, "trace-id"),
                Instant.now());
    }

    private String header(ConsumerRecord<String, String> record, String name) {
        Header header = record.headers().lastHeader(name);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
