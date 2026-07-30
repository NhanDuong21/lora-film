package com.project.notificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.notificationservice.domain.NotificationTypes.Category;
import com.project.notificationservice.domain.NotificationTypes.Channel;
import com.project.notificationservice.domain.NotificationTypes.DeliveryStatus;
import com.project.notificationservice.domain.NotificationTypes.Priority;
import com.project.notificationservice.domain.NotificationTypes.RequestStatus;
import com.project.notificationservice.entity.NotificationDelivery;
import com.project.notificationservice.entity.NotificationPreference;
import com.project.notificationservice.entity.NotificationRecipient;
import com.project.notificationservice.entity.NotificationRequest;
import com.project.notificationservice.repository.NotificationDeliveryRepository;
import com.project.notificationservice.repository.NotificationPreferenceRepository;
import com.project.notificationservice.repository.NotificationRecipientRepository;
import com.project.notificationservice.repository.NotificationRequestRepository;
import com.project.notificationservice.service.NotificationCommands.CreateNotificationCommand;
import com.project.notificationservice.service.NotificationCommands.RecipientCommand;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationApplicationServiceTest {

    private NotificationRequestRepository requestRepository;
    private NotificationRecipientRepository recipientRepository;
    private NotificationDeliveryRepository deliveryRepository;
    private NotificationPreferenceRepository preferenceRepository;
    private NotificationApplicationService service;

    @BeforeEach
    void setUp() {
        requestRepository = mock(NotificationRequestRepository.class);
        recipientRepository = mock(NotificationRecipientRepository.class);
        deliveryRepository = mock(NotificationDeliveryRepository.class);
        preferenceRepository = mock(NotificationPreferenceRepository.class);
        RecipientCryptoService crypto = mock(RecipientCryptoService.class);
        when(crypto.encrypt(any())).thenReturn("encrypted");
        when(requestRepository.save(any())).thenAnswer(invocation -> {
            NotificationRequest request = invocation.getArgument(0);
            ReflectionTestUtils.setField(request, "id", 10L);
            request.beforeInsert();
            return request;
        });
        when(recipientRepository.save(any())).thenAnswer(invocation -> {
            NotificationRecipient recipient = invocation.getArgument(0);
            ReflectionTestUtils.setField(recipient, "id", 20L);
            return recipient;
        });
        when(deliveryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service = new NotificationApplicationService(
                requestRepository, recipientRepository, deliveryRepository, preferenceRepository,
                crypto, new ObjectMapper(), new SimpleMeterRegistry());
    }

    @Test
    void marketingOptOutSuppressesMarketingDelivery() {
        NotificationPreference preference = new NotificationPreference();
        preference.setEnabled(false);
        when(preferenceRepository.findByUserPublicIdAndChannelAndCategory(
                "user", Channel.EMAIL, Category.MARKETING.name()))
                .thenReturn(Optional.of(preference));
        org.mockito.ArgumentCaptor<NotificationDelivery> captor =
                org.mockito.ArgumentCaptor.forClass(NotificationDelivery.class);

        service.accept(command(Category.MARKETING));

        org.mockito.Mockito.verify(deliveryRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(DeliveryStatus.SUPPRESSED);
        assertThat(captor.getValue().getFailureCode()).isEqualTo("MARKETING_OPT_OUT");
    }

    @Test
    void marketingOptOutDoesNotBlockTransactionalTicket() {
        NotificationPreference preference = new NotificationPreference();
        preference.setEnabled(false);
        when(preferenceRepository.findByUserPublicIdAndChannelAndCategory(
                "user", Channel.EMAIL, Category.MARKETING.name()))
                .thenReturn(Optional.of(preference));
        org.mockito.ArgumentCaptor<NotificationDelivery> captor =
                org.mockito.ArgumentCaptor.forClass(NotificationDelivery.class);

        service.accept(command(Category.TRANSACTIONAL));

        org.mockito.Mockito.verify(deliveryRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(DeliveryStatus.PENDING);
    }

    @Test
    void repeatedIdempotencyKeyReturnsExistingRequestWithoutCreatingAnother() {
        NotificationRequest existing = new NotificationRequest();
        existing.setPublicId("existing");
        existing.setStatus(RequestStatus.COMPLETED);
        ReflectionTestUtils.setField(existing, "id", 99L);
        when(requestRepository.findByIdempotencyKey("ticket-1")).thenReturn(Optional.of(existing));
        when(deliveryRepository.findByNotificationRequestIdOrderByCreatedAtAsc(99L))
                .thenReturn(List.of(new NotificationDelivery()));

        NotificationCommands.AcceptedNotification result = service.accept(command(Category.TRANSACTIONAL));

        assertThat(result.idempotent()).isTrue();
        assertThat(result.publicId()).isEqualTo("existing");
        org.mockito.Mockito.verify(requestRepository, org.mockito.Mockito.never()).save(any());
    }

    private CreateNotificationCommand command(Category category) {
        return new CreateNotificationCommand(
                "ticket-1", "booking-service", "event-1", "TICKET_PURCHASED",
                "correlation", null, "TICKET_PURCHASED", "vi-VN", category,
                Priority.HIGH, null, null, false,
                new RecipientCommand("user", "customer@example.com", null, null),
                Set.of(Channel.EMAIL),
                Map.of("bookingCode", "BK-1"));
    }
}
