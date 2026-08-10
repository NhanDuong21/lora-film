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
    private UserRecipientClient userRecipientClient;
    private NotificationEventConsumer consumer;

    @BeforeEach
    void setUp() {
        inboxRepository = mock(NotificationEventInboxRepository.class);
        applicationService = mock(NotificationApplicationService.class);
        userRecipientClient = mock(UserRecipientClient.class);
        consumer = new NotificationEventConsumer(
                objectMapper, inboxRepository, applicationService, userRecipientClient,
                "http://localhost:5173");
        when(inboxRepository.findBySourceServiceAndSourceEventId(anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(userRecipientClient.findByUserPublicId(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void ticketIssuedUsesBookingConfirmedTemplateFromGitCatalog() throws Exception {
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
        assertThat(captor.getValue().templateKey()).isEqualTo("BOOKING_CONFIRMED");
        assertThat(captor.getValue().eventType()).isEqualTo("TICKET_ISSUED");
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
    void ticketIssuedResolvesEmailFromUserServiceWhenEventOnlyContainsUserId() throws Exception {
        when(userRecipientClient.findByUserPublicId("42"))
                .thenReturn(Optional.of(new UserRecipientClient.ResolvedRecipient(
                        "customer@example.com", "Nguyen Van A")));
        DomainEventEnvelope event = new DomainEventEnvelope(
                "ticket-with-user-id",
                "ticket.issued",
                1,
                Instant.now(),
                "booking-service",
                null,
                null,
                "booking-public-id",
                "42",
                "vi-VN",
                Map.of("bookingCode", "BK-42", "customerName", "Customer"));

        consumer.consumeBookingEvent(objectMapper.writeValueAsString(event));

        ArgumentCaptor<NotificationCommands.CreateNotificationCommand> captor =
                ArgumentCaptor.forClass(NotificationCommands.CreateNotificationCommand.class);
        verify(applicationService).accept(captor.capture());
        assertThat(captor.getValue().recipient().email()).isEqualTo("customer@example.com");
        assertThat(captor.getValue().channels()).contains(
                com.project.notificationservice.domain.NotificationTypes.Channel.EMAIL);
        assertThat(captor.getValue().payload().get("customerName")).isEqualTo("Nguyen Van A");
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

    @Test
    void voucherGrantedResolvesCustomerAndCreatesEmailAndInAppNotification() throws Exception {
        when(userRecipientClient.findByUserPublicId("customer-42"))
                .thenReturn(Optional.of(new UserRecipientClient.ResolvedRecipient(
                        "customer@example.com", "Nguyen Van A")));
        String event = objectMapper.writeValueAsString(Map.of(
                "eventId", "voucher-event",
                "eventType", "VOUCHER_GRANTED",
                "data", Map.of(
                        "userPublicId", "customer-42",
                        "voucherCode", "CPN-1234",
                        "voucherName", "Member offer",
                        "discountType", "FIXED_AMOUNT",
                        "discountValue", 50000,
                        "minimumOrderAmount", 150000,
                        "validTo", "2099-12-31T23:59:59Z",
                        "deepLink", "/booking")));

        consumer.consumePromotionEvent(event);

        ArgumentCaptor<NotificationCommands.VoucherGrantedNotification> captor =
                ArgumentCaptor.forClass(NotificationCommands.VoucherGrantedNotification.class);
        verify(applicationService).acceptVoucherGranted(captor.capture());
        assertThat(captor.getValue().userPublicId()).isEqualTo("customer-42");
        assertThat(captor.getValue().email()).isEqualTo("customer@example.com");
        assertThat(captor.getValue().userName()).isEqualTo("Nguyen Van A");
        assertThat(captor.getValue().voucherCode()).isEqualTo("CPN-1234");
        assertThat(captor.getValue().discountValue()).contains("50.000");
        assertThat(captor.getValue().minimumOrderAmount()).contains("150.000");
        assertThat(captor.getValue().expiryDate()).isEqualTo("01/01/2100 06:59");
        assertThat(captor.getValue().useNowLink())
                .isEqualTo("http://localhost:5173/booking");
    }
}
