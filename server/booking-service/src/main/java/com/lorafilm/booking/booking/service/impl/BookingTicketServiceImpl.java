package com.lorafilm.booking.booking.service.impl;

import com.lorafilm.booking.booking.dto.BookingTicketDto;
import com.lorafilm.booking.booking.dto.CreateTicketRequest;
import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.entity.BookingTicket;
import com.lorafilm.booking.booking.enums.TicketStatus;
import com.lorafilm.booking.booking.mapper.BookingTicketMapper;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.booking.repository.BookingTicketRepository;
import com.lorafilm.booking.booking.service.BookingTicketService;
import com.lorafilm.booking.common.exception.BookingNotFoundException;
import com.lorafilm.booking.common.exception.BusinessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookingTicketServiceImpl implements BookingTicketService {

    private final BookingTicketRepository bookingTicketRepository;
    private final BookingRepository bookingRepository;
    private final BookingTicketMapper bookingTicketMapper;

    public BookingTicketServiceImpl(BookingTicketRepository bookingTicketRepository,
                                    BookingRepository bookingRepository,
                                    BookingTicketMapper bookingTicketMapper) {
        this.bookingTicketRepository = bookingTicketRepository;
        this.bookingRepository = bookingRepository;
        this.bookingTicketMapper = bookingTicketMapper;
    }

    @Override
    @Transactional
    public List<BookingTicketDto> createTickets(Long bookingId, List<CreateTicketRequest> requests) {
        if (bookingId == null) {
            throw new BusinessException("INVALID_BOOKING_ID", "Booking ID cannot be null");
        }
        if (requests == null || requests.isEmpty()) {
            throw new BusinessException("TICKETS_EMPTY", "Ticket list cannot be null or empty");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        List<BookingTicket> tickets = requests.stream().map(req -> {
            if (req.getSeatId() == null) {
                throw new BusinessException("INVALID_TICKET_DATA", "Seat ID is required for ticket creation");
            }
            BookingTicket ticket = bookingTicketMapper.toEntity(req);
            ticket.setBooking(booking);
            String code = "TK-" + (booking.getBookingCode() != null ? booking.getBookingCode() : bookingId) + "-" + req.getSeatId();
            ticket.setTicketCode(code);
            if (ticket.getPublicId() == null) {
                ticket.setPublicId(UUID.randomUUID().toString());
            }
            return ticket;
        }).collect(Collectors.toList());

        List<BookingTicket> savedTickets = bookingTicketRepository.saveAll(tickets);
        return savedTickets.stream().map(bookingTicketMapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteTickets(Long bookingId) {
        if (bookingId == null) {
            throw new BusinessException("INVALID_BOOKING_ID", "Booking ID cannot be null");
        }
        if (!bookingRepository.existsById(bookingId)) {
            throw new BookingNotFoundException(bookingId);
        }

        List<BookingTicket> tickets = bookingTicketRepository.findByBookingId(bookingId);
        if (tickets.isEmpty()) {
            return;
        }

        for (BookingTicket ticket : tickets) {
            ticket.setStatus(TicketStatus.CANCELLED);
        }
        bookingTicketRepository.saveAll(tickets);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingTicketDto> findTickets(Pageable pageable) {
        return bookingTicketRepository.findAll(pageable).map(bookingTicketMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingTicketDto> findByBooking(Long bookingId) {
        if (bookingId == null) {
            throw new BusinessException("INVALID_BOOKING_ID", "Booking ID cannot be null");
        }
        if (!bookingRepository.existsById(bookingId)) {
            throw new BookingNotFoundException(bookingId);
        }

        List<BookingTicket> tickets = bookingTicketRepository.findByBookingId(bookingId);
        if (tickets.isEmpty()) {
            throw new BusinessException("NO_TICKETS_FOUND", "Booking has no tickets");
        }

        return tickets.stream().map(bookingTicketMapper::toDto).collect(Collectors.toList());
    }
}
