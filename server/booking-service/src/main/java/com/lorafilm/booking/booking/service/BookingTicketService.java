package com.lorafilm.booking.booking.service;

import com.lorafilm.booking.booking.dto.BookingTicketDto;
import com.lorafilm.booking.booking.dto.CreateTicketRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BookingTicketService {

    List<BookingTicketDto> createTickets(Long bookingId, List<CreateTicketRequest> requests);

    void deleteTickets(Long bookingId);

    Page<BookingTicketDto> findTickets(Pageable pageable);

    List<BookingTicketDto> findByBooking(Long bookingId);
}
