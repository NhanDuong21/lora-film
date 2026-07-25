package com.lorafilm.booking.service;

import com.lorafilm.booking.audit.service.BookingAuditService;
import com.lorafilm.booking.audit.service.BookingOperationLogService;
import com.lorafilm.booking.booking.dto.BookingAdminResponse;
import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.mapper.BookingMapper;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.booking.service.BookingStatusHistoryService;
import com.lorafilm.booking.booking.service.BookingStatusTransitionService;
import com.lorafilm.booking.booking.service.impl.InternalBookingServiceImpl;
import com.lorafilm.booking.common.exception.BookingNotFoundException;
import com.lorafilm.booking.infrastructure.service.BookingOutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class InternalBookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingStatusHistoryService historyService;
    @Mock
    private BookingAuditService auditService;
    @Mock
    private BookingOperationLogService operationLogService;
    @Mock
    private BookingOutboxService outboxService;

    @Mock
    private com.lorafilm.booking.infrastructure.monitoring.BookingMetricsManager bookingMetricsManager;
    @Mock
    private com.lorafilm.booking.booking.service.BookingTicketService ticketService;

    private BookingMapper bookingMapper = new BookingMapper();
    private BookingStatusTransitionService statusTransitionService = new BookingStatusTransitionService();

    private InternalBookingServiceImpl internalBookingService;

    private Booking sampleBooking;

    @BeforeEach
    public void setUp() {
        internalBookingService = new InternalBookingServiceImpl(
                bookingRepository,
                bookingMapper,
                statusTransitionService,
                historyService,
                auditService,
                operationLogService,
                outboxService,
                bookingMetricsManager,
                ticketService
        );

        sampleBooking = new Booking();
        sampleBooking.setId(10L);
        sampleBooking.setPublicId("550e8400-e29b-41d4-a716-446655440000");
        sampleBooking.setBookingCode("BK1001");
        sampleBooking.setExpiresAt(java.time.Instant.now().plusSeconds(3600));
        ReflectionTestUtils.setField(sampleBooking, "bookingStatus", BookingStatus.PENDING_PAYMENT);
    }

    @Test
    public void confirmBooking_Success() {
        when(bookingRepository.findByPublicId("550e8400-e29b-41d4-a716-446655440000")).thenReturn(Optional.of(sampleBooking));
        when(bookingRepository.save(any())).thenReturn(sampleBooking);

        BookingAdminResponse result = internalBookingService.confirmBooking("550e8400-e29b-41d4-a716-446655440000");

        assertNotNull(result);
        verify(historyService).saveHistory(eq(sampleBooking), eq("PENDING_PAYMENT"), eq("CONFIRMED"), any(), eq("INTERNAL_SERVICE"), eq("SYSTEM"));
        verify(auditService).logAudit(eq(10L), eq("SYSTEM"), eq("CONFIRM_BOOKING"), eq("bookingStatus"), eq("PENDING_PAYMENT"), eq("CONFIRMED"), any(), any(), any(), any());
        verify(operationLogService).logOperation(eq(10L), eq("CONFIRM_BOOKING"), eq("SYSTEM"), eq(true), eq(0L), any(), any(), any());
        verify(outboxService).createOutboxEvent(eq("BOOKING"), eq(10L), eq("BOOKING_CONFIRMED"), eq(sampleBooking));
    }

    @Test
    public void expireBooking_Success() {
        sampleBooking.setExpiresAt(java.time.Instant.now().minusSeconds(10));
        when(bookingRepository.findByPublicId("550e8400-e29b-41d4-a716-446655440000")).thenReturn(Optional.of(sampleBooking));
        when(bookingRepository.save(any())).thenReturn(sampleBooking);

        BookingAdminResponse result = internalBookingService.expireBooking("550e8400-e29b-41d4-a716-446655440000");

        assertNotNull(result);
        verify(outboxService).createOutboxEvent(eq("BOOKING"), eq(10L), eq("BOOKING_EXPIRED"), eq(sampleBooking));
    }

    @Test
    public void refundBooking_Success() {
        ReflectionTestUtils.setField(sampleBooking, "bookingStatus", BookingStatus.CONFIRMED);

        when(bookingRepository.findByPublicId("550e8400-e29b-41d4-a716-446655440000")).thenReturn(Optional.of(sampleBooking));
        when(bookingRepository.save(any())).thenReturn(sampleBooking);

        BookingAdminResponse result = internalBookingService.refundBooking("550e8400-e29b-41d4-a716-446655440000");

        assertNotNull(result);
        verify(outboxService).createOutboxEvent(eq("BOOKING"), eq(10L), eq("BOOKING_REFUNDED"), eq(sampleBooking));
    }

    @Test
    public void getBookingByCode_Success() {
        when(bookingRepository.findByBookingCode("BK1001")).thenReturn(Optional.of(sampleBooking));

        BookingAdminResponse result = internalBookingService.getBookingByCode("BK1001");

        assertNotNull(result);
        assertEquals("BK1001", result.getBookingCode());
    }

    @Test
    public void getBookingByCode_NotFound_ThrowsException() {
        when(bookingRepository.findByBookingCode("INVALID")).thenReturn(Optional.empty());

        assertThrows(BookingNotFoundException.class, () ->
                internalBookingService.getBookingByCode("INVALID"));
    }

}
