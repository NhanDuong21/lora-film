package com.project.bookingservice.service;

import com.project.bookingservice.dto.payment.ConfirmPaymentRequest;
import com.project.bookingservice.dto.payment.ConfirmPaymentResponse;
import com.project.bookingservice.dto.payment.FailPaymentRequest;
import com.project.bookingservice.entity.Booking;
import com.project.bookingservice.entity.SeatReservation;
import com.project.bookingservice.entity.Ticket;
import com.project.bookingservice.enumtype.BookingStatus;
import com.project.bookingservice.enumtype.ReservationStatus;
import com.project.bookingservice.exception.BusinessException;
import com.project.bookingservice.repository.BookingRepository;
import com.project.bookingservice.repository.SeatReservationRepository;
import com.project.bookingservice.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.project.bookingservice.service.lock.SeatLockManager;
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

    public InternalPaymentService(BookingRepository bookingRepository,
                                  SeatReservationRepository seatReservationRepository,
                                  TicketRepository ticketRepository,
                                  SeatLockManager seatLockManager) {
        this.bookingRepository = bookingRepository;
        this.seatReservationRepository = seatReservationRepository;
        this.ticketRepository = ticketRepository;
        this.seatLockManager = seatLockManager;
    }

    @Transactional
    public ConfirmPaymentResponse confirmPayment(Long bookingId, ConfirmPaymentRequest request, String idempotencyKey) {
        logger.info("Confirming payment for bookingId: {}", bookingId);

        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new BusinessException("BOOKING_NOT_FOUND", "Booking not found"));

            if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
                if (booking.getStatus() == BookingStatus.CONFIRMED) {
                    throw new BusinessException("BOOKING_PAYMENT_CONFIRMATION_CONFLICT", "Booking is already confirmed");
                }
                throw new BusinessException("BOOKING_INVALID_STATUS_TRANSITION", "Booking cannot be confirmed from status: " + booking.getStatus());
            }

            if (booking.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new BusinessException("BOOKING_PAYMENT_EXPIRED", "Booking payment period has expired");
            }

            if (booking.getTotalAmount().compareTo(request.getPaidAmount()) != 0) {
                throw new BusinessException("BOOKING_PAYMENT_AMOUNT_MISMATCH", "Paid amount does not match booking total");
            }

            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);

            List<SeatReservation> reservations = seatReservationRepository.findByBookingIdAndStatus(bookingId, ReservationStatus.CONVERTED);
            List<Ticket> generatedTickets = new ArrayList<>();
            List<ConfirmPaymentResponse.TicketResponse> ticketResponses = new ArrayList<>();

            BigDecimal ticketPrice = booking.getTotalAmount();
            if (!reservations.isEmpty()) {
                ticketPrice = booking.getTotalAmount().divide(new BigDecimal(reservations.size()), 2, RoundingMode.HALF_UP);
            }

            for (SeatReservation res : reservations) {
                Ticket ticket = new Ticket();
                ticket.setBookingId(bookingId);
                ticket.setSeatId(res.getSeatId());
                ticket.setPrice(ticketPrice);
                generatedTickets.add(ticket);

                // Clear Redis Seat Lock if still exists
                seatLockManager.evictLocks(res.getShowtimeId(), List.of(res.getSeatId()));
            }

            generatedTickets = ticketRepository.saveAll(generatedTickets);

            for (Ticket t : generatedTickets) {
                ConfirmPaymentResponse.TicketResponse tr = new ConfirmPaymentResponse.TicketResponse();
                tr.setTicketId(t.getId());
                tr.setSeatId(t.getSeatId());
                tr.setPrice(t.getPrice());
                ticketResponses.add(tr);
            }

            ConfirmPaymentResponse response = new ConfirmPaymentResponse();
            response.setBookingId(booking.getId());
            response.setBookingCode(booking.getBookingCode());
            response.setStatus(booking.getStatus());
            response.setConfirmedAt(LocalDateTime.now());
            response.setTickets(ticketResponses);

            return response;

        } catch (Exception e) {
            throw e;
        }
    }

    @Transactional
    public void failPayment(Long bookingId, FailPaymentRequest request, String idempotencyKey) {
        logger.info("Failing payment for bookingId: {}", bookingId);

        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new BusinessException("BOOKING_NOT_FOUND", "Booking not found"));

            if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
                // Do not transition to EXPIRED automatically unless it's past the deadline.
                // Just log the failure and allow retry until it expires.
                logger.info("Payment failed for bookingId: {}. Reason: {}. Will allow retry until {}", bookingId, request.getReason(), booking.getExpiresAt());
            }

        } catch (Exception e) {
            throw e;
        }
    }
}
