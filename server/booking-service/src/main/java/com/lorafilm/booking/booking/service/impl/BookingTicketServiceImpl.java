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
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import com.lorafilm.booking.booking.entity.BookingSnapshot;
import com.lorafilm.booking.booking.repository.BookingSnapshotRepository;
import com.lorafilm.booking.booking.client.ShowtimeBookingContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@Service
public class BookingTicketServiceImpl implements BookingTicketService {

    private final BookingTicketRepository bookingTicketRepository;
    private final BookingRepository bookingRepository;
    private final BookingTicketMapper bookingTicketMapper;
    private final BookingSnapshotRepository bookingSnapshotRepository;
    private final ObjectMapper objectMapper;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BookingTicketServiceImpl.class);

    public BookingTicketServiceImpl(BookingTicketRepository bookingTicketRepository,
                                    BookingRepository bookingRepository,
                                    BookingTicketMapper bookingTicketMapper,
                                    BookingSnapshotRepository bookingSnapshotRepository,
                                    ObjectMapper objectMapper) {
        this.bookingTicketRepository = bookingTicketRepository;
        this.bookingRepository = bookingRepository;
        this.bookingTicketMapper = bookingTicketMapper;
        this.bookingSnapshotRepository = bookingSnapshotRepository;
        this.objectMapper = objectMapper;
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
    @Transactional
    public void refundTickets(Long bookingId) {
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
            ticket.setStatus(TicketStatus.REFUNDED);
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
        return tickets.stream().map(bookingTicketMapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void generateTicketsForConfirmedBooking(Long bookingId) {
        log.info("Generating tickets for confirmed booking ID: {}", bookingId);
        if (!bookingTicketRepository.findByBookingId(bookingId).isEmpty()) {
            log.info("Tickets already generated for booking ID: {}. Skipping.", bookingId);
            return;
        }

        Optional<BookingSnapshot> snapshotOpt = bookingSnapshotRepository.findByBookingId(bookingId);
        if (snapshotOpt.isEmpty()) {
            log.warn("Snapshot not found for booking ID: {}. Skipping ticket generation.", bookingId);
            return;
        }
        BookingSnapshot snapshot = snapshotOpt.get();

        if (snapshot.getSnapshotJson() == null || snapshot.getSnapshotJson().isBlank()) {
            log.warn("Snapshot JSON is empty for booking ID: {}. Cannot generate tickets.", bookingId);
            return;
        }

        List<ShowtimeBookingContext.SeatContext> seats;
        try {
            seats = objectMapper.readValue(snapshot.getSnapshotJson(), new TypeReference<List<ShowtimeBookingContext.SeatContext>>() {});
        } catch (Exception e) {
            throw new BusinessException("SNAPSHOT_DESERIALIZATION_FAILED", "Failed to deserialize seat snapshot data: " + e.getMessage());
        }

        List<CreateTicketRequest> ticketRequests = seats.stream().map(seat -> {
            CreateTicketRequest req = new CreateTicketRequest();
            req.setSeatId(seat.seatId());
            req.setSeatLabel(seat.seatLabel());
            req.setSeatType(seat.seatType());
            req.setTicketPrice(seat.price());
            req.setMovieTitle(snapshot.getMovieTitle());
            req.setCinemaName(snapshot.getCinemaName());
            req.setAuditoriumName(snapshot.getAuditoriumName());
            req.setShowtimeStart(snapshot.getShowtimeStart());
            req.setShowtimeEnd(snapshot.getShowtimeEnd());
            return req;
        }).toList();

        createTickets(bookingId, ticketRequests);
        log.info("Successfully generated {} tickets for booking ID: {}", ticketRequests.size(), bookingId);
    }
}
