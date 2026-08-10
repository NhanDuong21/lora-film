package com.project.scoreservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.scoreservice.dto.ScoreEarnRequest;
import com.project.scoreservice.dto.ScoreEarnResponse;
import com.project.scoreservice.service.ScoreService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingConfirmedScoreConsumerTest {
    private ScoreService scoreService;
    private BookingConfirmedScoreConsumer consumer;

    @BeforeEach
    void setUp() {
        scoreService = mock(ScoreService.class);
        consumer = new BookingConfirmedScoreConsumer(new ObjectMapper(), scoreService);
    }

    @Test
    void awardsPointsFromAuthoritativeConfirmedBooking() {
        when(scoreService.earnPoints(any())).thenReturn(new ScoreEarnResponse(
                2, 0, 2, 0, 2, "SILVER", "SILVER", false, false));
        ConsumerRecord<String, String> record = record("""
                {
                  "id": 20,
                  "publicId": "1a3b9827-2138-4241-bb71-c8a95fdec833",
                  "userId": 3,
                  "bookingStatus": "CONFIRMED",
                  "paymentStatus": "SUCCESS",
                  "finalAmount": 49000.00
                }
                """);
        record.headers().add(
                "event-id",
                "9976b220-57e7-4472-abc6-336449d75269"
                        .getBytes(StandardCharsets.UTF_8));

        consumer.onBookingEvent(record);

        ArgumentCaptor<ScoreEarnRequest> request =
                ArgumentCaptor.forClass(ScoreEarnRequest.class);
        verify(scoreService).earnPoints(request.capture());
        assertEquals(3L, request.getValue().userId());
        assertEquals(20L, request.getValue().bookingId());
        assertTrue(new BigDecimal("49000.00")
                .compareTo(request.getValue().eligibleAmount()) == 0);
        assertEquals("9976b220-57e7-4472-abc6-336449d75269",
                request.getValue().eventId());
        assertEquals("EARN:BOOKING:20", request.getValue().idempotencyKey());
    }

    @Test
    void ignoresBookingEventsThatAreNotConfirmedAndPaid() {
        consumer.onBookingEvent(record("""
                {
                  "id": 20,
                  "userId": 3,
                  "bookingStatus": "PENDING_PAYMENT",
                  "paymentStatus": "PENDING",
                  "finalAmount": 49000
                }
                """));

        verify(scoreService, never()).earnPoints(any());
    }

    @Test
    void rejectsMalformedConfirmedBookingEventsForDeadLetterHandling() {
        assertThrows(IllegalArgumentException.class, () ->
                consumer.onBookingEvent(record("""
                        {
                          "bookingStatus": "CONFIRMED",
                          "paymentStatus": "SUCCESS",
                          "finalAmount": 49000
                        }
                        """)));
        verify(scoreService, never()).earnPoints(any());
    }

    private ConsumerRecord<String, String> record(String payload) {
        return new ConsumerRecord<>(
                "booking.events.v1", 0, 0L, "booking-key", payload);
    }
}
