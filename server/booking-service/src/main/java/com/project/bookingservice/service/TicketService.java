package com.project.bookingservice.service;

import com.project.bookingservice.dto.response.TicketResponse;
import com.project.bookingservice.entity.Booking;
import com.project.bookingservice.entity.Ticket;
import com.project.bookingservice.enumtype.BookingStatus;
import com.project.bookingservice.exception.BusinessException;
import com.project.bookingservice.repository.BookingRepository;
import com.project.bookingservice.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final BookingRepository bookingRepository;

    public TicketService(TicketRepository ticketRepository, BookingRepository bookingRepository) {
        this.ticketRepository = ticketRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getTicketsByBookingId(Long bookingId, Long currentUserId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException("BOOKING_NOT_FOUND", "Booking not found"));

        if (!booking.getUserId().equals(currentUserId)) {
            throw new BusinessException("FORBIDDEN", "Forbidden: You don't have permission to access this booking's tickets");
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            return Collections.emptyList();
        }

        List<Ticket> tickets = ticketRepository.findByBookingId(bookingId);
        return tickets.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicketDetails(Long ticketId, Long currentUserId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BusinessException("TICKET_NOT_FOUND", "Ticket not found"));

        Booking booking = bookingRepository.findById(ticket.getBookingId())
                .orElseThrow(() -> new BusinessException("BOOKING_NOT_FOUND", "Booking not found for ticket"));

        if (!booking.getUserId().equals(currentUserId)) {
            throw new BusinessException("FORBIDDEN", "Forbidden: You don't have permission to access this ticket");
        }

        return mapToResponse(ticket);
    }

    private TicketResponse mapToResponse(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getBookingId(),
                ticket.getSeatId(),
                ticket.getPrice(),
                ticket.getCreatedAt()
        );
    }
}
