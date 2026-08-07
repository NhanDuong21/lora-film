package com.project.userservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.userservice.dto.AccountVerifiedEvent;
import com.project.userservice.dto.AccountVerifiedPayload;
import com.project.userservice.entity.User;
import com.project.userservice.entity.CustomerProfile;
import com.project.userservice.repository.CustomerProfileRepository;
import com.project.userservice.repository.UserRepository;
import com.project.userservice.service.UserAuditService;
import com.project.userservice.service.UserDomainEventService;
import com.project.userservice.service.impl.ReservationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountVerifiedConsumerOAuthTest {

    @Test
    void createsProfileWithGoogleNameEmailAndAvatar() throws Exception {
        Dependencies dependencies = new Dependencies();
        AccountVerifiedConsumer consumer = dependencies.consumer();
        AccountVerifiedEvent event = oauthEvent();
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        when(dependencies.objectMapper.readValue("event", AccountVerifiedEvent.class))
                .thenReturn(event);
        when(dependencies.userRepository.findById(31L)).thenReturn(Optional.empty());
        when(dependencies.customerProfileRepository.existsByAccountId(31L)).thenReturn(true);

        consumer.consume("event", acknowledgment);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(dependencies.userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals(31L, savedUser.getAccountId());
        assertEquals("Google Customer", savedUser.getFullName());
        assertEquals("customer@example.com", savedUser.getEmail());
        assertEquals("https://example.com/google-avatar.jpg", savedUser.getAvatarUrl());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void doesNotOverwriteProfileFieldsEditedByUser() throws Exception {
        Dependencies dependencies = new Dependencies();
        AccountVerifiedConsumer consumer = dependencies.consumer();
        AccountVerifiedEvent event = oauthEvent();
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        User existingUser = new User();
        existingUser.setAccountId(31L);
        existingUser.setEmail("custom@example.com");
        existingUser.setFullName("Custom Name");
        existingUser.setAvatarUrl("https://example.com/custom-avatar.jpg");
        when(dependencies.objectMapper.readValue("event", AccountVerifiedEvent.class))
                .thenReturn(event);
        when(dependencies.userRepository.findById(31L)).thenReturn(Optional.of(existingUser));
        when(dependencies.customerProfileRepository.existsByAccountId(31L)).thenReturn(true);

        consumer.consume("event", acknowledgment);

        assertEquals("Custom Name", existingUser.getFullName());
        assertEquals("custom@example.com", existingUser.getEmail());
        assertEquals("https://example.com/custom-avatar.jpg", existingUser.getAvatarUrl());
        verify(dependencies.userRepository, never()).save(existingUser);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void releasesRegistrationReservationUsingNormalizedEmailOwner() throws Exception {
        Dependencies dependencies = new Dependencies();
        AccountVerifiedConsumer consumer = dependencies.consumer();
        AccountVerifiedEvent event = oauthEvent();
        event.getData().setPhoneNumber("0901234567");
        event.getData().setCccd("092205006789");
        event.getData().setEmail(" Customer@Example.COM ");
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        User existingUser = new User();
        existingUser.setAccountId(31L);

        when(dependencies.objectMapper.readValue("event", AccountVerifiedEvent.class))
                .thenReturn(event);
        when(dependencies.userRepository.findById(31L)).thenReturn(Optional.of(existingUser));
        when(dependencies.customerProfileRepository.existsByAccountId(31L)).thenReturn(true);

        consumer.consume("event", acknowledgment);

        verify(dependencies.reservationService).release(
                "0901234567", "092205006789", "customer@example.com");
        verify(acknowledgment).acknowledge();
    }

    @Test
    void workforceAccountDoesNotCreateCustomerPersona() throws Exception {
        Dependencies dependencies = new Dependencies();
        AccountVerifiedConsumer consumer = dependencies.consumer();
        AccountVerifiedEvent event = oauthEvent();
        event.getData().setRole("EMPLOYEE");
        Acknowledgment acknowledgment = mock(Acknowledgment.class);

        when(dependencies.objectMapper.readValue("event", AccountVerifiedEvent.class))
                .thenReturn(event);
        when(dependencies.userRepository.findById(31L)).thenReturn(Optional.empty());

        consumer.consume("event", acknowledgment);

        verify(dependencies.customerProfileRepository, never()).save(any(CustomerProfile.class));
        verify(dependencies.customerProfileRepository, never()).existsByAccountId(31L);
        verify(acknowledgment).acknowledge();
    }

    private AccountVerifiedEvent oauthEvent() {
        AccountVerifiedPayload payload = new AccountVerifiedPayload();
        payload.setRequestId("oauth-request");
        payload.setAccountId(31L);
        payload.setEmail("customer@example.com");
        payload.setRole("CUSTOMER");
        payload.setFullName("Google Customer");
        payload.setAvatarUrl("https://example.com/google-avatar.jpg");

        AccountVerifiedEvent event = new AccountVerifiedEvent();
        event.setEventId("event-id");
        event.setEventType("ACCOUNT_VERIFIED");
        event.setData(payload);
        return event;
    }

    private static final class Dependencies {
        private final UserRepository userRepository = mock(UserRepository.class);
        private final ReservationService reservationService = mock(ReservationService.class);
        private final ObjectMapper objectMapper = mock(ObjectMapper.class);
        private final CustomerProfileRepository customerProfileRepository =
                mock(CustomerProfileRepository.class);
        private final UserDomainEventService eventService = mock(UserDomainEventService.class);
        private final UserAuditService auditService = mock(UserAuditService.class);

        private AccountVerifiedConsumer consumer() {
            return new AccountVerifiedConsumer(
                    userRepository,
                    reservationService,
                    objectMapper,
                    customerProfileRepository,
                    eventService,
                    auditService);
        }
    }
}
