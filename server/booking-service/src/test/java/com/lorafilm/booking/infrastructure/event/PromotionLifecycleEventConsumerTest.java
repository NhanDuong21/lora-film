package com.lorafilm.booking.infrastructure.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.infrastructure.entity.BookingInboxEvent;
import com.lorafilm.booking.infrastructure.repository.BookingInboxEventRepository;
import com.lorafilm.booking.infrastructure.service.PromotionConfirmationReconciliationService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PromotionLifecycleEventConsumerTest {

    private final BookingInboxEventRepository inboxRepository =
            mock(BookingInboxEventRepository.class);
    private final PromotionConfirmationReconciliationService reconciliationService =
            mock(PromotionConfirmationReconciliationService.class);
    private final PromotionLifecycleEventConsumer consumer =
            new PromotionLifecycleEventConsumer(
                    inboxRepository, reconciliationService, new ObjectMapper());

    @Test
    void confirmedPromotionCreatesAnIdempotentReconciliationObservation() {
        String eventId = "11111111-1111-4111-8111-111111111111";
        when(inboxRepository.findByEventId(eventId)).thenReturn(Optional.empty());
        when(inboxRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        consumer.consume(event(eventId));

        verify(reconciliationService).observeLifecycleEvent(
                "22222222-2222-4222-8222-222222222222",
                "33333333-3333-4333-8333-333333333333",
                "44444444-4444-4444-8444-444444444444",
                "RESERVATION_CONFIRMED");
        var inboxCaptor = org.mockito.ArgumentCaptor.forClass(BookingInboxEvent.class);
        verify(inboxRepository, org.mockito.Mockito.times(2)).save(inboxCaptor.capture());
        assertThat(inboxCaptor.getValue().getProcessed()).isTrue();
    }

    @Test
    void processedPromotionEventIsIgnoredOnKafkaRedelivery() {
        String eventId = "11111111-1111-4111-8111-111111111111";
        BookingInboxEvent existing = new BookingInboxEvent();
        existing.setEventId(eventId);
        existing.setProcessed(true);
        when(inboxRepository.findByEventId(eventId)).thenReturn(Optional.of(existing));

        consumer.consume(event(eventId));

        verify(reconciliationService, never()).observeLifecycleEvent(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void orderOnlyPromotionEventIsIgnoredWithoutCreatingInboxEntry() {
        consumer.consume("""
                {
                  "eventId":"55555555-5555-4555-8555-555555555555",
                  "eventType":"RESERVATION_CONFIRMED",
                  "aggregateType":"PROMOTION_RESERVATION",
                  "data":{
                    "publicId":"66666666-6666-4666-8666-666666666666",
                    "orderPublicId":"77777777-7777-4777-8777-777777777777"
                  }
                }
                """);

        verify(inboxRepository, never()).findByEventId(
                org.mockito.ArgumentMatchers.anyString());
        verify(reconciliationService, never()).observeLifecycleEvent(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private String event(String eventId) {
        return """
                {
                  "eventId":"%s",
                  "eventType":"RESERVATION_CONFIRMED",
                  "aggregateType":"PROMOTION_RESERVATION",
                  "data":{
                    "publicId":"22222222-2222-4222-8222-222222222222",
                    "bookingPublicId":"33333333-3333-4333-8333-333333333333",
                    "paymentPublicId":"44444444-4444-4444-8444-444444444444"
                  }
                }
                """.formatted(eventId);
    }
}
