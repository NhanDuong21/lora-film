package com.lorafilm.booking.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.enums.PaymentStatus;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.booking.repository.BookingStatusHistoryRepository;
import com.lorafilm.booking.booking.service.BookingService;
import com.lorafilm.booking.infrastructure.entity.BookingInboxEvent;
import com.lorafilm.booking.infrastructure.repository.BookingInboxEventRepository;
import com.lorafilm.booking.payment.dto.InitiatePaymentRequest;
import com.lorafilm.booking.payment.dto.PaymentResponseDto;
import com.lorafilm.booking.payment.event.PaymentEventConsumer;
import com.lorafilm.booking.payment.event.contract.PaymentEvent;
import com.lorafilm.booking.payment.event.contract.PaymentEventPayload;
import com.lorafilm.booking.payment.repository.BookingPaymentEventRepository;
import com.lorafilm.booking.security.service.SecurityContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class PaymentIntegrationTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private PaymentEventConsumer consumer;

    @Autowired
    private BookingPaymentEventRepository paymentEventRepository;

    @Autowired
    private BookingInboxEventRepository inboxEventRepository;

    @Autowired
    private BookingStatusHistoryRepository statusHistoryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SecurityContextService securityContextService;

    private Booking testBooking;

    @BeforeEach
    public void setUp() {
        when(securityContextService.getCurrentUserId()).thenReturn(1L);

        // Create and save a test booking in PENDING_PAYMENT state
        testBooking = Booking.create(
                UUID.randomUUID().toString(),
                "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                1L, // userId
                2L, // showtimeId
                3L, // movieId
                4L, // cinemaId
                5L, // auditoriumId
                new BigDecimal("150000.00"),
                BigDecimal.ZERO,
                new BigDecimal("5000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "VND",
                Instant.now().plusSeconds(300),
                "Test notes"
        );
        testBooking = bookingRepository.saveAndFlush(testBooking);
    }

    @Test
    public void testInitiatePayment_Success() {
        InitiatePaymentRequest request = new InitiatePaymentRequest("MOMO", "Momo");
        PaymentResponseDto response = bookingService.initiatePayment(testBooking.getPublicId(), request);

        assertNotNull(response);
        assertEquals("PENDING", response.paymentStatus());
        assertEquals(testBooking.getId(), response.bookingId());
        assertNotNull(response.paymentUrl());

        // Check booking has updated snapshots
        Booking updatedBooking = bookingRepository.findById(testBooking.getId()).orElseThrow();
        assertEquals("MOMO", updatedBooking.getPaymentMethodSnapshot());
        assertEquals("Momo", updatedBooking.getPaymentProvider());
        assertEquals(response.transactionCode(), updatedBooking.getPaymentReference());

        // Check payment event snapshot is created
        var paymentEvents = paymentEventRepository.findByBookingId(testBooking.getId());
        assertFalse(paymentEvents.isEmpty());
        assertEquals(response.transactionCode(), paymentEvents.get(0).getTransactionId());
    }

    @Test
    @Transactional
    public void testConsumePaymentSuccess_ConfirmBooking() throws Exception {
        PaymentEventPayload payload = new PaymentEventPayload(
                12345L,
                testBooking.getId(),
                "MOCK-TXN-123",
                "MOMO",
                "SUCCESS",
                new BigDecimal("155000.00"),
                "VND",
                "GATEWAY-REF-123",
                null,
                null
        );

        PaymentEvent event = new PaymentEvent(
                UUID.randomUUID().toString(),
                "PAYMENT_SUCCESS",
                String.valueOf(testBooking.getId()),
                "BOOKING",
                1,
                Instant.now(),
                "Momo",
                "v1.0",
                UUID.randomUUID().toString(),
                payload
        );

        String eventJson = objectMapper.writeValueAsString(event);
        consumer.consume(eventJson);

        // Check inbox event created and marked processed
        Optional<BookingInboxEvent> inboxRecord = inboxEventRepository.findByEventId(event.eventId());
        assertTrue(inboxRecord.isPresent());
        assertTrue(inboxRecord.get().getProcessed());

        // Check booking updated to CONFIRMED and payment status to SUCCESS
        Booking updatedBooking = bookingRepository.findById(testBooking.getId()).orElseThrow();
        assertEquals(BookingStatus.CONFIRMED, updatedBooking.getBookingStatus());
        assertEquals(PaymentStatus.SUCCESS, updatedBooking.getPaymentStatus());

        // Check status history logged
        var histories = statusHistoryRepository.findByBookingId(testBooking.getId());
        assertFalse(histories.isEmpty());
        assertEquals("PENDING_PAYMENT", histories.get(0).getFromStatus());
        assertEquals("CONFIRMED", histories.get(0).getToStatus());
    }

    @Test
    @Transactional
    public void testConsumePaymentSuccess_DuplicateHandling() throws Exception {
        PaymentEventPayload payload = new PaymentEventPayload(
                12345L,
                testBooking.getId(),
                "MOCK-TXN-123",
                "MOMO",
                "SUCCESS",
                new BigDecimal("155000.00"),
                "VND",
                "GATEWAY-REF-123",
                null,
                null
        );

        PaymentEvent event = new PaymentEvent(
                UUID.randomUUID().toString(),
                "PAYMENT_SUCCESS",
                String.valueOf(testBooking.getId()),
                "BOOKING",
                1,
                Instant.now(),
                "Momo",
                "v1.0",
                UUID.randomUUID().toString(),
                payload
        );

        String eventJson = objectMapper.writeValueAsString(event);
        consumer.consume(eventJson);

        // First execution: Confirm booking is CONFIRMED
        Booking updatedBooking = bookingRepository.findById(testBooking.getId()).orElseThrow();
        assertEquals(BookingStatus.CONFIRMED, updatedBooking.getBookingStatus());

        // Count histories
        int initialHistorySize = statusHistoryRepository.findByBookingId(testBooking.getId()).size();

        // Second execution (duplicate eventId)
        consumer.consume(eventJson);

        // History count should NOT increase
        int secondaryHistorySize = statusHistoryRepository.findByBookingId(testBooking.getId()).size();
        assertEquals(initialHistorySize, secondaryHistorySize);
    }
}
