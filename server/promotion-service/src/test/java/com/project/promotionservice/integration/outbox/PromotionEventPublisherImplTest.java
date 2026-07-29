package com.project.promotionservice.integration.outbox;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PromotionEventPublisherImplTest {

    @Test
    void publishReturnsOnlyAfterBrokerAcknowledgement() {
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        CompletableFuture<SendResult<String, String>> acknowledgement =
                CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(acknowledgement);
        PromotionEventPublisherImpl publisher =
                new PromotionEventPublisherImpl(kafkaTemplate, 1);

        publisher.publish(event());

        verify(kafkaTemplate).send(any(ProducerRecord.class));
    }

    @Test
    void failedBrokerAcknowledgementIsNotReportedAsPublished() {
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        CompletableFuture<SendResult<String, String>> acknowledgement =
                CompletableFuture.failedFuture(new IllegalStateException("broker unavailable"));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(acknowledgement);
        PromotionEventPublisherImpl publisher =
                new PromotionEventPublisherImpl(kafkaTemplate, 1);

        assertThatThrownBy(() -> publisher.publish(event()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("did not acknowledge");
    }

    private PromotionOutboxEvent event() {
        PromotionOutboxEvent event = new PromotionOutboxEvent();
        event.setTopicName("promotion.reservation.lifecycle");
        event.setEventKey("reservation-id");
        event.setEventType("PROMOTION_RESERVED");
        event.setPayload("{\"schemaVersion\":\"1.0\"}");
        return event;
    }
}
