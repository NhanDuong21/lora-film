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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;
import java.util.Map;
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
    private com.lorafilm.booking.infrastructure.service.BookingOutboxService outboxService;
    private String ticketAccessBaseUrl = "http://localhost:5173/tickets";
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

    @Autowired(required = false)
    public void setOutboxService(com.lorafilm.booking.infrastructure.service.BookingOutboxService outboxService) {
        this.outboxService = outboxService;
    }

    @Value("${booking.ticket-access-base-url:http://localhost:5173/tickets}")
    public void setTicketAccessBaseUrl(String ticketAccessBaseUrl) {
        this.ticketAccessBaseUrl = ticketAccessBaseUrl;
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
            String code = "TK-" + (booking.getBookingCode() != null ? booking.getBookingCode() : bookingId) + "-"
                    + req.getSeatId();
            ticket.setTicketCode(code);
            if (ticket.getPublicId() == null) {
                ticket.setPublicId(UUID.randomUUID().toString());
            }
            if (ticket.getQrCode() == null) {
                ticket.setQrCode(UUID.randomUUID().toString());
            }
            if (ticket.getBarcode() == null) {
                ticket.setBarcode("LF-" + UUID.randomUUID().toString().replace("-", "").toUpperCase());
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
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

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
            seats = objectMapper.readValue(snapshot.getSnapshotJson(),
                    new TypeReference<List<ShowtimeBookingContext.SeatContext>>() {
                    });
        } catch (Exception e) {
            throw new BusinessException("SNAPSHOT_DESERIALIZATION_FAILED",
                    "Failed to deserialize seat snapshot data: " + e.getMessage());
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

        List<BookingTicketDto> issuedTickets = createTickets(bookingId, ticketRequests);
        publishTicketIssued(booking, snapshot, issuedTickets);
        log.info("Successfully generated {} tickets for booking ID: {}", ticketRequests.size(), bookingId);
    }

    private void publishTicketIssued(
            Booking booking,
            BookingSnapshot snapshot,
            List<BookingTicketDto> tickets) {
        if (outboxService == null || tickets.isEmpty()) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        put(payload, "userPublicId", String.valueOf(booking.getUserId()));

        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.lorafilm.booking.security.principal.UserPrincipal principal) {
            if (principal.getEmail() != null && !principal.getEmail().isBlank()) {
                put(payload, "email", principal.getEmail());
            }
            if (principal.getUsername() != null && !principal.getUsername().isBlank()) {
                put(payload, "customerName", principal.getUsername());
            }
        } else {
            put(payload, "customerName", "Customer");
        }

        put(payload, "bookingPublicId", booking.getPublicId());
        put(payload, "bookingCode", booking.getBookingCode());
        put(payload, "paymentCode", booking.getPaymentReference());
        put(payload, "movieTitle", snapshot.getMovieTitle());
        put(payload, "moviePosterUrl", snapshot.getMoviePoster());
        put(payload, "movieAgeRating", snapshot.getAgeRating());
        put(payload, "cinemaName", snapshot.getCinemaName());
        put(payload, "auditoriumName", snapshot.getAuditoriumName());
        put(payload, "showtime", snapshot.getShowtimeStart());
        put(payload, "seatNames", tickets.stream().map(BookingTicketDto::getSeatLabel).toList());
        put(payload, "ticketCodes", tickets.stream().map(BookingTicketDto::getTicketCode).toList());
        put(payload, "ticketTypes", tickets.stream().map(BookingTicketDto::getSeatType).toList());
        put(payload, "ticketPublicIds", tickets.stream().map(BookingTicketDto::getPublicId).toList());
        put(payload, "foodItems", foodItems(booking));
        put(payload, "promotionCode", snapshot.getPromotionCode());
        put(payload, "promotionName", snapshot.getPromotionName());
        put(payload, "subtotal", booking.getTicketAmount().add(booking.getFoodAmount())
                .add(booking.getServiceFee()).add(booking.getTaxAmount()));
        put(payload, "discount", booking.getPromotionDiscount().add(booking.getVoucherDiscount()));
        put(payload, "totalPaid", booking.getFinalAmount());
        put(payload, "totalAmount", booking.getFinalAmount());
        put(payload, "currency", booking.getCurrency());
        put(payload, "paymentMethod", booking.getPaymentMethodSnapshot());
        put(payload, "paymentTime", booking.getConfirmedAt());
        put(payload, "deepLink", "/bookings/" + booking.getPublicId());
        // Per-ticket list: each entry has seatLabel, seatType, ticketCode,
        // ticketAccessUrl
        List<Map<String, Object>> ticketEntries = tickets.stream().map(t -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            put(entry, "seatLabel", t.getSeatLabel());
            put(entry, "seatType", t.getSeatType());
            put(entry, "ticketCode", t.getTicketCode());
            String accessUrl = ticketAccessBaseUrl + "/" + t.getPublicId() + "?access=" + t.getQrCode();
            put(entry, "ticketAccessUrl", accessUrl);
            return entry;
        }).toList();
        put(payload, "tickets", ticketEntries);
        // Keep single ticketAccessUrl (first ticket) for legacy templates
        BookingTicketDto first = tickets.getFirst();
        put(payload, "ticketAccessUrl", ticketAccessBaseUrl + "/" + first.getPublicId()
                + "?access=" + first.getQrCode());
        put(payload, "locale", "vi-VN");
        outboxService.createOutboxEvent("Booking", booking.getId(), "TICKET_ISSUED", payload);
    }

    private List<Map<String, Object>> foodItems(Booking booking) {
        if (booking.getFoodOrder() == null) {
            return List.of();
        }
        return booking.getFoodOrder().getItems().stream().map(item -> {
            Map<String, Object> food = new LinkedHashMap<>();
            put(food, "name", item.getProductName());
            put(food, "quantity", item.getQuantity());
            put(food, "unitPrice", item.getUnitPrice());
            put(food, "total", item.getFinalAmount());
            return food;
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public void resendBookingEmail(String publicId) {
        log.info("Resending booking email for public ID: {}", publicId);
        Booking booking = bookingRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BookingNotFoundException(java.util.UUID.fromString(publicId)));

        Long bookingId = booking.getId();
        BookingSnapshot snapshot = bookingSnapshotRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new BusinessException("SNAPSHOT_NOT_FOUND",
                        "Snapshot not found for booking ID: " + bookingId));

        List<BookingTicket> tickets = bookingTicketRepository.findByBookingId(bookingId);
        if (tickets.isEmpty()) {
            throw new BusinessException("TICKETS_NOT_FOUND", "No tickets found for booking ID: " + bookingId);
        }

        List<BookingTicketDto> ticketDtos = tickets.stream()
                .map(bookingTicketMapper::toDto)
                .collect(Collectors.toList());

        publishTicketIssued(booking, snapshot, ticketDtos);
    }

    private void put(Map<String, Object> payload, String key, Object value) {
        if (value != null) {
            payload.put(key, value);
        }
    }
}
