package com.lorafilm.booking.service;

import com.lorafilm.booking.audit.service.BookingAuditService;
import com.lorafilm.booking.audit.service.BookingOperationLogService;
import com.lorafilm.booking.booking.dto.BookingAdminResponse;
import com.lorafilm.booking.booking.dto.BookingDetailResponse;
import com.lorafilm.booking.booking.dto.BookingFilterRequest;
import com.lorafilm.booking.booking.dto.UpdateBookingStatusRequest;
import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.mapper.BookingMapper;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.booking.service.BookingSnapshotService;
import com.lorafilm.booking.booking.service.BookingStatusHistoryService;
import com.lorafilm.booking.booking.service.BookingStatusTransitionService;
import com.lorafilm.booking.booking.service.BookingTicketService;
import com.lorafilm.booking.booking.service.impl.AdminBookingServiceImpl;
import com.lorafilm.booking.common.exception.BookingNotFoundException;
import com.lorafilm.booking.common.exception.BusinessException;
import com.lorafilm.booking.common.response.PagedResponse;
import com.lorafilm.booking.infrastructure.service.BookingOutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AdminBookingServiceTest {

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
    private BookingTicketService ticketService;
    @Mock
    private BookingSnapshotService snapshotService;

    private BookingMapper bookingMapper = new BookingMapper();
    private BookingStatusTransitionService statusTransitionService = new BookingStatusTransitionService();

    private AdminBookingServiceImpl adminBookingService;

    private Booking sampleBooking;

    @BeforeEach
    public void setUp() {
        adminBookingService = new AdminBookingServiceImpl(
                bookingRepository,
                bookingMapper,
                statusTransitionService,
                historyService,
                auditService,
                operationLogService,
                outboxService,
                ticketService,
                snapshotService
        );

        sampleBooking = new Booking();
        sampleBooking.setId(10L);
        sampleBooking.setBookingCode("BK1001");
        sampleBooking.setBookingStatus(BookingStatus.PENDING_PAYMENT);
    }

    @Test
    public void findBookings_Success() {
        BookingFilterRequest filter = new BookingFilterRequest();
        filter.setPage(0);
        filter.setSize(10);

        when(bookingRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleBooking)));

        PagedResponse<BookingAdminResponse> result = adminBookingService.findBookings(filter);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    public void getBookingDetail_Success() {
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(sampleBooking));

        BookingDetailResponse result = adminBookingService.getBookingDetail(10L);

        assertNotNull(result);
        assertEquals("BK1001", result.getBookingCode());
    }

    @Test
    public void updateBookingStatus_Success_FlowExecuted() {
        UpdateBookingStatusRequest request = new UpdateBookingStatusRequest(BookingStatus.CONFIRMED, "Payment done", "ADMIN", "Note");

        when(bookingRepository.findById(10L)).thenReturn(Optional.of(sampleBooking));
        when(bookingRepository.save(any())).thenReturn(sampleBooking);

        BookingAdminResponse response = adminBookingService.updateBookingStatus(10L, request);

        assertNotNull(response);
        verify(historyService).saveHistory(eq(sampleBooking), eq("PENDING_PAYMENT"), eq("CONFIRMED"), eq("Payment done"), eq("ADMIN"), eq("ADMIN"));
        verify(auditService).logAudit(eq(10L), eq("ADMIN"), eq("CHANGE_STATUS"), eq("bookingStatus"), eq("PENDING_PAYMENT"), eq("CONFIRMED"), any(), any(), any(), any());
        verify(operationLogService).logOperation(eq(10L), eq("CHANGE_STATUS"), eq("ADMIN"), eq(true), eq(0L), any(), any(), eq("Payment done"));
        verify(outboxService).createOutboxEvent(eq("BOOKING"), eq(10L), eq("BOOKING_CONFIRMED"), eq(sampleBooking));
    }

    @Test
    public void updateBookingStatus_InvalidTransition_ThrowsException() {
        UpdateBookingStatusRequest request = new UpdateBookingStatusRequest(BookingStatus.CONFIRMED, "Retry", "ADMIN", null);
        sampleBooking.setBookingStatus(BookingStatus.CANCELLED);

        when(bookingRepository.findById(10L)).thenReturn(Optional.of(sampleBooking));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminBookingService.updateBookingStatus(10L, request));
        assertEquals("CANNOT_CONFIRM_CANCELLED", ex.getErrorCode());
    }

    @Test
    public void updateBookingStatus_BookingNotFound_ThrowsException() {
        UpdateBookingStatusRequest request = new UpdateBookingStatusRequest(BookingStatus.CONFIRMED, "Reason", "ADMIN", null);
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BookingNotFoundException.class, () ->
                adminBookingService.updateBookingStatus(99L, request));
    }
}
