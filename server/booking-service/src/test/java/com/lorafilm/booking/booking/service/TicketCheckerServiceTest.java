package com.lorafilm.booking.booking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lorafilm.booking.booking.dto.ticketscan.TicketScanRequest;
import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.entity.BookingTicket;
import com.lorafilm.booking.booking.entity.TicketScanEvent;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.enums.PaymentStatus;
import com.lorafilm.booking.booking.enums.TicketScanResult;
import com.lorafilm.booking.booking.enums.TicketStatus;
import com.lorafilm.booking.booking.repository.BookingTicketRepository;
import com.lorafilm.booking.booking.repository.TicketGateHandoffRepository;
import com.lorafilm.booking.booking.repository.TicketScanEventRepository;
import com.lorafilm.booking.infrastructure.client.EmployeeCinemaScopeClient;
import com.lorafilm.booking.security.service.ManagerCinemaScopeService;
import com.lorafilm.booking.security.service.SecurityContextService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TicketCheckerServiceTest {

    @Mock private BookingTicketRepository ticketRepository;
    @Mock private TicketScanEventRepository eventRepository;
    @Mock private TicketGateHandoffRepository handoffRepository;
    @Mock private EmployeeCinemaScopeClient cinemaScopeClient;
    @Mock private ManagerCinemaScopeService managerCinemaScopeService;
    @Mock private SecurityContextService securityContextService;

    private TicketCheckerService service;

    @BeforeEach
    void setUp() {
        service = new TicketCheckerService(ticketRepository, eventRepository, handoffRepository,
                cinemaScopeClient, managerCinemaScopeService, securityContextService,
                "Asia/Ho_Chi_Minh", 30);
    }

    @Test
    void admitsActivePaidTicketAndStoresOperatorTrace() {
        prepareEmployeeScope();
        BookingTicket ticket = activeTicket("cinema-1");
        when(ticketRepository.findByAnyCodeForUpdate("QR-001")).thenReturn(Optional.of(ticket));

        var response = service.scan(new TicketScanRequest("QR-001", "Cửa phòng 01"));

        assertTrue(response.admitted());
        assertEquals(TicketScanResult.ADMITTED, response.result());
        assertEquals(TicketStatus.USED, ticket.getStatus());
        assertEquals(5L, ticket.getUsedByAccountId());
        assertEquals("cinema-1", ticket.getUsedCinemaPublicId());
        assertEquals("Cửa phòng 01", ticket.getUsedGateLabel());
        assertNotNull(ticket.getUsedAt());
        verify(ticketRepository).save(ticket);
    }

    @Test
    void rejectsDuplicateWithoutChangingTicketAgain() {
        prepareEmployeeScope();
        BookingTicket ticket = activeTicket("cinema-1");
        ticket.setStatus(TicketStatus.USED);
        ticket.setUsedAt(Instant.now().minusSeconds(20));
        when(ticketRepository.findByAnyCodeForUpdate("QR-001")).thenReturn(Optional.of(ticket));

        var response = service.scan(new TicketScanRequest("QR-001", "Cửa phòng 01"));

        assertFalse(response.admitted());
        assertEquals(TicketScanResult.ALREADY_USED, response.result());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void rejectsTicketFromAnotherCinemaAndKeepsItActive() {
        prepareEmployeeScope();
        BookingTicket ticket = activeTicket("cinema-2");
        when(ticketRepository.findByAnyCodeForUpdate("QR-001")).thenReturn(Optional.of(ticket));

        var response = service.scan(new TicketScanRequest("QR-001", "Sảnh chính"));

        assertFalse(response.admitted());
        assertEquals(TicketScanResult.WRONG_CINEMA, response.result());
        assertEquals(TicketStatus.ACTIVE, ticket.getStatus());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void managerSummaryUsesOnlyTheSelectedAssignedCinema() {
        LocalDate date = LocalDate.of(2026, 8, 10);
        when(managerCinemaScopeService.requireAssigned("cinema-1")).thenReturn("cinema-1");
        when(eventRepository
                .findByCinemaPublicIdAndScannedAtGreaterThanEqualAndScannedAtLessThanOrderByScannedAtDesc(
                        any(), any(), any(), any()))
                .thenReturn(List.of());
        when(ticketRepository.findOperationalTickets(any(), any(), any())).thenReturn(List.of());

        var response = service.managerSummary("cinema-1", date);

        assertEquals(date, response.date());
        assertEquals(0, response.totalScans());
        assertEquals(0, response.totalTickets());
        verify(managerCinemaScopeService).requireAssigned("cinema-1");
    }

    private void prepareEmployeeScope() {
        when(securityContextService.getCurrentUserId()).thenReturn(5L);
        when(cinemaScopeClient.requireActiveCinema(5L)).thenReturn("cinema-1");
        when(eventRepository.save(any(TicketScanEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private BookingTicket activeTicket(String cinemaPublicId) {
        Booking booking = new Booking();
        booking.setPublicId("booking-public-1");
        booking.setBookingCode("LORAFILM-001");
        booking.setCinemaPublicId(cinemaPublicId);
        booking.setExpiresAt(Instant.now().plusSeconds(600));
        booking.changeStatus(BookingStatus.CONFIRMED, Instant.now());
        booking.setPaymentStatus(PaymentStatus.SUCCESS);
        booking.setShowtimePublicId("showtime-public-1");

        BookingTicket ticket = new BookingTicket();
        ticket.setPublicId("ticket-public-1");
        ticket.setTicketCode("TK-001");
        ticket.setQrCode("QR-001");
        ticket.setStatus(TicketStatus.ACTIVE);
        ticket.setBooking(booking);
        ticket.setMovieTitle("Người Nhện: Khởi Đầu Mới");
        ticket.setCinemaName("LoraFilm Landmark 81");
        ticket.setAuditoriumName("Phòng 01 - Tiêu chuẩn");
        ticket.setSeatLabel("A1");
        ticket.setShowtimeStart(Instant.now().minusSeconds(300));
        ticket.setShowtimeEnd(Instant.now().plusSeconds(7_200));
        return ticket;
    }
}
