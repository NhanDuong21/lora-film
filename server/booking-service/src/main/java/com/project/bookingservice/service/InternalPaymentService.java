package com.project.bookingservice.service;

import com.project.bookingservice.dto.movie.ShowtimeInfo;
import com.project.bookingservice.dto.payment.PaymentContextResponse;
import com.project.bookingservice.dto.payment.PaymentResultRequest;
import com.project.bookingservice.dto.payment.PaymentResultResponse;
import com.project.bookingservice.entity.Booking;
import com.project.bookingservice.entity.BookingPaymentResultEvent;
import com.project.bookingservice.entity.SeatReservation;
import com.project.bookingservice.entity.Ticket;
import com.project.bookingservice.enumtype.BookingStatus;
import com.project.bookingservice.enumtype.ReservationStatus;
import com.project.bookingservice.exception.BusinessException;
import com.project.bookingservice.repository.BookingPaymentResultEventRepository;
import com.project.bookingservice.repository.BookingRepository;
import com.project.bookingservice.repository.SeatReservationRepository;
import com.project.bookingservice.repository.TicketRepository;
import com.project.bookingservice.service.lock.SeatLockManager;
import com.project.bookingservice.service.movie.MovieServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class InternalPaymentService {

    private static final Logger logger = LoggerFactory.getLogger(InternalPaymentService.class);
    private final BookingRepository bookingRepository;
    private final SeatReservationRepository seatReservationRepository;
    private final TicketRepository ticketRepository;
    private final SeatLockManager seatLockManager;
    private final BookingPaymentResultEventRepository eventRepository;
    private final MovieServiceClient movieServiceClient;

    public InternalPaymentService(BookingRepository bookingRepository,
                                  SeatReservationRepository seatReservationRepository,
                                  TicketRepository ticketRepository,
                                  SeatLockManager seatLockManager,
                                  BookingPaymentResultEventRepository eventRepository,
                                  MovieServiceClient movieServiceClient) {
        this.bookingRepository = bookingRepository;
        this.seatReservationRepository = seatReservationRepository;
        this.ticketRepository = ticketRepository;
        this.seatLockManager = seatLockManager;
        this.eventRepository = eventRepository;
        this.movieServiceClient = movieServiceClient;
    }

    @Transactional(readOnly = true)
    public PaymentContextResponse getPaymentContext(Long bookingId) {
        logger.info("Fetching payment context for bookingId: {}", bookingId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException("BOOKING_NOT_FOUND", "Booking not found"));

        boolean payable = booking.getStatus() == BookingStatus.PENDING_PAYMENT 
                && booking.getExpiresAt().isAfter(LocalDateTime.now());

        if (!payable) {
            throw new BusinessException("BOOKING_NOT_PAYABLE", "Booking is not payable");
        }

        PaymentContextResponse response = new PaymentContextResponse();
        response.setBookingId(booking.getId());
        response.setAccountId(booking.getUserId());
        response.setBookingStatus(booking.getStatus());
        response.setPayable(payable);
        response.setAmount(booking.getTotalAmount());
        response.setCurrency("VND");
        response.setExpiresAt(booking.getExpiresAt());

        // Get Analytics Snapshot
        ShowtimeInfo showtime = movieServiceClient.getShowtime(booking.getShowtimeId());
        
        List<SeatReservation> reservations = seatReservationRepository.findByBookingIdAndStatus(bookingId, ReservationStatus.CONVERTED);
        int ticketCount = reservations.size();
        
        PaymentContextResponse.AnalyticsSnapshot snapshot = new PaymentContextResponse.AnalyticsSnapshot(
                showtime.getMovieId(),
                showtime.getMovieTitle(),
                ticketCount
        );
        response.setAnalyticsSnapshot(snapshot);

        return response;
    }

    @Transactional
    public PaymentResultResponse processPaymentResult(Long bookingId, PaymentResultRequest request) {
        logger.info("Processing payment result for bookingId: {}, eventId: {}", bookingId, request.getEventId());

        // 1. Idempotency check based on eventId
        if (eventRepository.findByEventId(request.getEventId()).isPresent()) {
            logger.info("Event {} already processed", request.getEventId());
            return new PaymentResultResponse(request.getEventId(), false, true, "ALREADY_PROCESSED");
        }

        // 2. Lock Booking and retrieve
        Booking booking = bookingRepository.findByIdWithPessimisticLock(bookingId)
                .orElseThrow(() -> new BusinessException("BOOKING_NOT_FOUND", "Booking not found"));

        if (!booking.getId().equals(bookingId)) { // wait, request doesn't have bookingId. The parameter does.
            // parameter is already used to fetch booking.
        }

        // 3. Check for amount and currency mismatch (only for SUCCESS attempts to confirm booking)
        if ("SUCCESS".equals(request.getResult())) {
            if (booking.getTotalAmount().compareTo(request.getAmount()) != 0) {
                throw new BusinessException("PAYMENT_AMOUNT_MISMATCH", "Paid amount does not match booking total");
            }
            if (!"VND".equals(request.getCurrency())) {
                throw new BusinessException("PAYMENT_CURRENCY_MISMATCH", "Currency does not match");
            }
        }

        BookingPaymentResultEvent event = new BookingPaymentResultEvent();
        event.setEventId(request.getEventId());
        event.setBookingId(bookingId);
        event.setPaymentId(request.getPaymentId());
        event.setPaymentTransactionCode(request.getPaymentTransactionCode());
        event.setPaymentMethod(request.getPaymentMethod());
        event.setPaymentResult(request.getResult());
        event.setAmount(request.getAmount());
        event.setCurrency(request.getCurrency());
        event.setExternalTransactionId(request.getExternalTransactionId());
        event.setReconciliationStatus(request.getReconciliationStatus());
        event.setOccurredAt(request.getOccurredAt());
        event.setProcessedAt(LocalDateTime.now());

        PaymentResultResponse response = new PaymentResultResponse();
        response.setEventId(request.getEventId());
        response.setDuplicate(false);

        // 4. State Transitions
        if ("SUCCESS".equals(request.getResult())) {
            if (booking.getStatus() == BookingStatus.CONFIRMED) {
                logger.info("Booking {} already confirmed by another payment", bookingId);
                event.setApplied(false);
                event.setAcknowledgementResult("ALREADY_CONFIRMED_BY_ANOTHER_PAYMENT");
                
                response.setApplied(false);
                response.setResult("ALREADY_CONFIRMED_BY_ANOTHER_PAYMENT");
            } else if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
                logger.info("Confirming booking {}", bookingId);
                
                booking.setStatus(BookingStatus.CONFIRMED);
                bookingRepository.save(booking);

                generateTicketsForBooking(booking);

                event.setApplied(true);
                event.setAcknowledgementResult("BOOKING_CONFIRMED");
                
                response.setApplied(true);
                response.setResult("BOOKING_CONFIRMED");
            } else {
                logger.info("Booking {} cannot be confirmed due to status {}", bookingId, booking.getStatus());
                event.setApplied(false);
                event.setAcknowledgementResult("BOOKING_INVALID_STATE");
                
                response.setApplied(false);
                response.setResult("BOOKING_INVALID_STATE");
            }
        } else { // FAILED, CANCELLED, EXPIRED
            logger.info("Payment result is {}, acknowledging but not changing booking {}", request.getResult(), bookingId);
            event.setApplied(false);
            event.setAcknowledgementResult("ACKNOWLEDGED_NO_ACTION");
            
            response.setApplied(false);
            response.setResult("ACKNOWLEDGED_NO_ACTION");
        }

        // 5. Save Deduplication Event
        eventRepository.save(event);
        return response;
    }

    private void generateTicketsForBooking(Booking booking) {
        List<SeatReservation> reservations = seatReservationRepository.findByBookingIdAndStatus(booking.getId(), ReservationStatus.CONVERTED);
        List<Ticket> generatedTickets = new ArrayList<>();

        BigDecimal ticketPrice = booking.getTotalAmount();
        if (!reservations.isEmpty()) {
            ticketPrice = booking.getTotalAmount().divide(new BigDecimal(reservations.size()), 2, RoundingMode.HALF_UP);
        }

        for (SeatReservation res : reservations) {
            Ticket ticket = new Ticket();
            ticket.setBookingId(booking.getId());
            ticket.setSeatId(res.getSeatId());
            ticket.setPrice(ticketPrice);
            generatedTickets.add(ticket);

            // Clear Redis Seat Lock if still exists
            seatLockManager.evictLocks(res.getShowtimeId(), List.of(res.getSeatId()));
        }

        ticketRepository.saveAll(generatedTickets);
    }
}
