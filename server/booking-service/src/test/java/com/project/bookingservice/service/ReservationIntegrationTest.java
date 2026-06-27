package com.project.bookingservice.service;

import com.project.bookingservice.dto.reservation.CreateReservationRequest;
import com.project.bookingservice.dto.reservation.ReservationResponse;
import com.project.bookingservice.entity.SeatReservation;
import com.project.bookingservice.repository.SeatReservationRepository;
import com.project.bookingservice.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class ReservationIntegrationTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private SeatReservationRepository seatReservationRepository;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    @Test
    void testEndToEndReservationFlow() {
        // Arrange
        when(currentUserProvider.getCurrentUserId()).thenReturn(99L);
        CreateReservationRequest request = new CreateReservationRequest(10L, List.of(201L, 202L));
        String idempotencyKey = "integration-test-key";

        // Act
        List<ReservationResponse> responses = reservationService.createReservation(request, idempotencyKey);

        // Assert
        assertNotNull(responses);
        assertEquals(2, responses.size());
        
        List<SeatReservation> saved = seatReservationRepository.findActiveReservations(10L, List.of(201L, 202L));
        assertEquals(2, saved.size());
        
        // Test idempotency replay
        List<ReservationResponse> responses2 = reservationService.createReservation(request, idempotencyKey);
        assertEquals(responses.size(), responses2.size());
    }
}
