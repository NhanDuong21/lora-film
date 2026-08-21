package com.lorafilm.booking.infrastructure.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.infrastructure.enums.OutboxStatus;
import com.lorafilm.booking.infrastructure.monitoring.BookingMetricsManager;
import com.lorafilm.booking.infrastructure.repository.BookingOutboxEventRepository;
import com.lorafilm.booking.infrastructure.service.impl.BookingOutboxServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BookingPromotionOutboxContractTest {

    @Test
    void confirmedBookingPersistsAllFieldsRequiredByPromotionAutomation() throws Exception {
        BookingOutboxEventRepository repository = mock(BookingOutboxEventRepository.class);
        BookingMetricsManager metrics = mock(BookingMetricsManager.class);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ObjectMapper objectMapper = new ObjectMapper();
        BookingOutboxServiceImpl outbox = new BookingOutboxServiceImpl(
                repository, objectMapper, metrics);
        Booking booking = new Booking();
        booking.setId(7L);
        booking.setPublicId("booking-1");
        booking.setFirstConfirmedBooking(true);
        booking.setTicketIssued(true);
        booking.setAutomationCustomerId(42L);
        booking.setAutomationEligible(true);
        booking.setFinalAmount(new BigDecimal("150000.00"));

        var event = outbox.createOutboxEvent(
                "BOOKING", booking.getId(), "BOOKING_CONFIRMED", booking);
        JsonNode payload = objectMapper.readTree(event.getPayload());

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getRetryCount()).isZero();
        assertThat(event.getAggregatePublicId()).isEqualTo("booking-1");
        assertThat(payload.path("publicId").asText()).isEqualTo("booking-1");
        assertThat(payload.path("firstConfirmedBooking").asBoolean()).isTrue();
        assertThat(payload.path("ticketIssued").asBoolean()).isTrue();
        assertThat(payload.path("automationCustomerId").asLong()).isEqualTo(42L);
        assertThat(payload.path("automationEligible").asBoolean()).isTrue();
        verify(metrics).incrementOutboxCreated();
    }
}
