package com.lorafilm.booking.service;

import com.lorafilm.booking.booking.dto.BookingTicketDto;
import com.lorafilm.booking.booking.dto.CreateTicketRequest;
import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.entity.BookingPriceSnapshot;
import com.lorafilm.booking.booking.entity.BookingSnapshot;
import com.lorafilm.booking.booking.entity.BookingTicket;
import com.lorafilm.booking.booking.enums.TicketStatus;
import com.lorafilm.booking.booking.mapper.BookingTicketMapper;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.booking.repository.BookingPriceSnapshotRepository;
import com.lorafilm.booking.booking.repository.BookingTicketRepository;
import com.lorafilm.booking.booking.service.impl.BookingTicketServiceImpl;
import com.lorafilm.booking.common.exception.BookingNotFoundException;
import com.lorafilm.booking.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BookingTicketServiceTest {

    @Mock
    private BookingTicketRepository bookingTicketRepository;

    @Mock
    private BookingRepository bookingRepository;

    private BookingTicketMapper bookingTicketMapper = new BookingTicketMapper();

    @Mock
    private com.lorafilm.booking.booking.repository.BookingSnapshotRepository bookingSnapshotRepository;

    @Mock
    private BookingPriceSnapshotRepository bookingPriceSnapshotRepository;

    @Mock
    private com.lorafilm.booking.infrastructure.service.BookingOutboxService outboxService;

    private BookingTicketServiceImpl bookingTicketService;

    private Booking sampleBooking;
    private CreateTicketRequest createRequest;
    private BookingTicket sampleTicket;

    @BeforeEach
    public void setUp() {
        bookingTicketService = new BookingTicketServiceImpl(bookingTicketRepository, bookingRepository, bookingTicketMapper,
                bookingSnapshotRepository, bookingPriceSnapshotRepository, new com.fasterxml.jackson.databind.ObjectMapper());
        bookingTicketService.setOutboxService(outboxService);

        sampleBooking = new Booking();
        sampleBooking.setId(10L);
        sampleBooking.setBookingCode("BK1001");

        createRequest = new CreateTicketRequest();
        createRequest.setSeatId(15L);
        createRequest.setSeatLabel("A1");
        createRequest.setTicketPrice(BigDecimal.valueOf(100000));

        sampleTicket = new BookingTicket();
        sampleTicket.setId(100L);
        sampleTicket.setBooking(sampleBooking);
        sampleTicket.setSeatId(15L);
        sampleTicket.setTicketCode("TK-BK1001-15");
        sampleTicket.setStatus(TicketStatus.ACTIVE);
    }

    @Test
    public void createTickets_Success() {
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(sampleBooking));
        when(bookingTicketRepository.saveAll(anyList())).thenReturn(List.of(sampleTicket));

        List<BookingTicketDto> result = bookingTicketService.createTickets(10L, List.of(createRequest));

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(bookingTicketRepository).saveAll(anyList());
    }

    @Test
    public void createTickets_NullBookingId_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                bookingTicketService.createTickets(null, List.of(createRequest)));
        assertEquals("INVALID_BOOKING_ID", ex.getErrorCode());
    }

    @Test
    public void createTickets_EmptyRequests_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                bookingTicketService.createTickets(10L, Collections.emptyList()));
        assertEquals("TICKETS_EMPTY", ex.getErrorCode());
    }

    @Test
    public void createTickets_BookingNotFound_ThrowsException() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BookingNotFoundException.class, () ->
                bookingTicketService.createTickets(99L, List.of(createRequest)));
    }

    @Test
    public void findByBooking_Success() {
        when(bookingRepository.existsById(10L)).thenReturn(true);
        when(bookingTicketRepository.findByBookingId(10L)).thenReturn(List.of(sampleTicket));

        List<BookingTicketDto> result = bookingTicketService.findByBooking(10L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void findByBooking_NoTickets_ReturnsEmptyCollection() {
        when(bookingRepository.existsById(10L)).thenReturn(true);
        when(bookingTicketRepository.findByBookingId(10L)).thenReturn(Collections.emptyList());

        List<BookingTicketDto> result = bookingTicketService.findByBooking(10L);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void deleteTickets_Success() {
        when(bookingRepository.existsById(10L)).thenReturn(true);
        when(bookingTicketRepository.findByBookingId(10L)).thenReturn(List.of(sampleTicket));

        bookingTicketService.deleteTickets(10L);

        assertEquals(TicketStatus.CANCELLED, sampleTicket.getStatus());
        verify(bookingTicketRepository).saveAll(anyList());
    }

    @Test
    public void deleteTickets_NoTickets_NoAction() {
        when(bookingRepository.existsById(10L)).thenReturn(true);
        when(bookingTicketRepository.findByBookingId(10L)).thenReturn(Collections.emptyList());

        // Should not throw any exception and should just return
        bookingTicketService.deleteTickets(10L);
    }

    @Test
    public void generateTickets_BoxOfficeSale_DoesNotNotifyEmployeeAccount() {
        sampleBooking.setPublicId("b0658f1e-8c5f-4f6e-93c0-9caa8d847f15");
        BookingSnapshot snapshot = new BookingSnapshot();
        snapshot.setSnapshotJson("[{\"seatId\":15,\"seatLabel\":\"A1\",\"seatType\":\"STANDARD\",\"price\":100000}]");
        BookingPriceSnapshot priceSnapshot = new BookingPriceSnapshot();
        priceSnapshot.setPricingBreakdownJson("{\"channel\":\"BOX_OFFICE\"}");

        when(bookingTicketRepository.findByBookingId(10L)).thenReturn(Collections.emptyList());
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(sampleBooking));
        when(bookingSnapshotRepository.findByBookingId(10L)).thenReturn(Optional.of(snapshot));
        when(bookingPriceSnapshotRepository.findByBookingId(10L)).thenReturn(Optional.of(priceSnapshot));
        when(bookingTicketRepository.saveAll(anyList())).thenReturn(List.of(sampleTicket));

        bookingTicketService.generateTicketsForConfirmedBooking(10L);

        verify(bookingTicketRepository).saveAll(anyList());
        verify(outboxService, never()).createOutboxEvent(any(), any(), any(), any());
    }
}
