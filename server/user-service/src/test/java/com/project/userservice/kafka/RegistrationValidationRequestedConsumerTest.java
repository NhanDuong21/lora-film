package com.project.userservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.userservice.dto.RegistrationValidationRequestedEvent;
import com.project.userservice.dto.RegistrationValidationRequestedPayload;
import com.project.userservice.dto.ReservationResult;
import com.project.userservice.repository.UserRepository;
import com.project.userservice.service.impl.ReservationService;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistrationValidationRequestedConsumerTest {

    @Test
    void usesNormalizedEmailAsStableReservationOwner() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        ReservationService reservationService = mock(ReservationService.class);
        UserEventPublisher eventPublisher = mock(UserEventPublisher.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        RegistrationValidationRequestedConsumer consumer =
                new RegistrationValidationRequestedConsumer(
                        userRepository, reservationService, eventPublisher, objectMapper);

        RegistrationValidationRequestedPayload payload = new RegistrationValidationRequestedPayload();
        payload.setRequestId("random-request-id");
        payload.setEmail(" Customer@Example.COM ");
        payload.setPhoneNumber("0901234567");
        payload.setCccd("092205006789");
        RegistrationValidationRequestedEvent event = new RegistrationValidationRequestedEvent();
        event.setData(payload);

        when(objectMapper.readValue("event", RegistrationValidationRequestedEvent.class))
                .thenReturn(event);
        when(reservationService.reserve(
                "0901234567",
                "092205006789",
                "customer@example.com",
                Duration.ofMinutes(15)))
                .thenReturn(new ReservationResult(true, null, null));

        consumer.consume("event");

        verify(reservationService).reserve(
                "0901234567",
                "092205006789",
                "customer@example.com",
                Duration.ofMinutes(15));
        verify(eventPublisher).publishRegistrationValidationResult(
                "random-request-id", "SUCCESS", null, null);
    }
}
