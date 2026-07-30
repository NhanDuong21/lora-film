package com.project.notificationservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.notificationservice.repository.NotificationEventInboxRepository;
import com.project.notificationservice.service.NotificationApplicationService;
import com.project.notificationservice.service.NotificationCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationEventConsumerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private NotificationEventInboxRepository inboxRepository;
    private NotificationApplicationService applicationService;
    private NotificationEventConsumer consumer;

    @BeforeEach
    void setUp() {
        inboxRepository = mock(NotificationEventInboxRepository.class);
        applicationService = mock(NotificationApplicationService.class);
        consumer = new NotificationEventConsumer(objectMapper, inboxRepository, applicationService);
        when(inboxRepository.findBySourceServiceAndSourceEventId(anyString(), anyString()))
                .thenReturn(Optional.empty());
    }

    @Test
    void ticketIssuedCreatesPurchasedTicketNotification() throws Exception {
        DomainEventEnvelope event = new DomainEventEnvelope(
                "1c99e5b9-cf0a-4b88-89d6-54ed6b36f10a",
                "ticket.issued",
                1,
                Instant.now(),
                "booking-service",
                "correlation",
                "cause",
                "booking-public-id",
                "user-public-id",
                "vi-VN",
                Map.of("email", "customer@example.com", "bookingCode", "BK-1"));

        consumer.consumeBookingEvent(objectMapper.writeValueAsString(event));

        ArgumentCaptor<NotificationCommands.CreateNotificationCommand> captor =
                ArgumentCaptor.forClass(NotificationCommands.CreateNotificationCommand.class);
        verify(applicationService).accept(captor.capture());
        assertThat(captor.getValue().templateKey()).isEqualTo("TICKET_PURCHASED");
        assertThat(captor.getValue().eventType()).isEqualTo("TICKET_PURCHASED");
        assertThat(captor.getValue().channels()).containsExactlyInAnyOrder(
                com.project.notificationservice.domain.NotificationTypes.Channel.EMAIL,
                com.project.notificationservice.domain.NotificationTypes.Channel.IN_APP);
    }

    @Test
    void paymentSucceededAloneDoesNotCreatePurchasedTicketNotification() throws Exception {
        DomainEventEnvelope event = new DomainEventEnvelope(
                "payment-event",
                "payment.succeeded",
                1,
                Instant.now(),
                "payment-service",
                null,
                null,
                "booking-public-id",
                "user-public-id",
                "vi-VN",
                Map.of());

        consumer.consumePaymentEvent(objectMapper.writeValueAsString(event));

        verify(applicationService, never()).accept(any());
    }

    @Test
    void duplicateTicketEventIsIgnoredByInbox() throws Exception {
        when(inboxRepository.existsBySourceServiceAndSourceEventId("booking-service", "duplicate"))
                .thenReturn(true);
        DomainEventEnvelope event = new DomainEventEnvelope(
                "duplicate", "ticket.issued", 1, Instant.now(), "booking-service",
                null, null, "booking", "user", "vi-VN", Map.of());

        consumer.consumeBookingEvent(objectMapper.writeValueAsString(event));

        verify(applicationService, never()).accept(any());
        verify(inboxRepository, never()).saveAndFlush(any());
    }
}
