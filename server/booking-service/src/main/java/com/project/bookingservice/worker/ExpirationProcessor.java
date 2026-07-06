package com.project.bookingservice.worker;

import com.project.bookingservice.entity.Booking;
import com.project.bookingservice.entity.SeatReservation;
import com.project.bookingservice.enumtype.BookingStatus;
import com.project.bookingservice.enumtype.ReservationStatus;
import com.project.bookingservice.repository.BookingRepository;
import com.project.bookingservice.repository.SeatReservationRepository;
import com.project.bookingservice.service.lock.SeatLockManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ExpirationProcessor {

    private final BookingRepository bookingRepository;
    private final SeatReservationRepository seatReservationRepository;
    private final SeatLockManager seatLockManager;

    public ExpirationProcessor(BookingRepository bookingRepository,
                               SeatReservationRepository seatReservationRepository,
                               SeatLockManager seatLockManager) {
        this.bookingRepository = bookingRepository;
        this.seatReservationRepository = seatReservationRepository;
        this.seatLockManager = seatLockManager;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processReservationExpiration(SeatReservation reservation) {
        if (reservation.getStatus() != ReservationStatus.HELD) {
            return;
        }

        reservation.setStatus(ReservationStatus.EXPIRED);
        seatReservationRepository.saveAndFlush(reservation);

        try {
            seatLockManager.evictLocks(reservation.getShowtimeId(), List.of(reservation.getSeatId()));
        } catch (Exception e) {
            // Log but don't fail transaction for Redis issues as DB is already updated
            // Wait, if Redis fails, the transaction is marked for rollback if we throw?
            // Actually, we catch it here to prevent throwing, but prompt says "if Redis is down or unreachable during the worker runtime, the worker must safely abort or fail-fast for the affected batch instead of forcing a database-only state transition that bypasses cache layer synchronization."
            // This means we should let the exception propagate so it rolls back, and the batch stops.
            throw e; 
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processBookingExpiration(Booking booking) {
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            return;
        }

        booking.setStatus(BookingStatus.EXPIRED);
        bookingRepository.saveAndFlush(booking);

        List<SeatReservation> reservations = seatReservationRepository.findByBookingId(booking.getId());
        for (SeatReservation res : reservations) {
            if (res.getStatus() == ReservationStatus.CONVERTED) {
                res.setStatus(ReservationStatus.EXPIRED);
                seatReservationRepository.saveAndFlush(res);
                
                seatLockManager.evictLocks(res.getShowtimeId(), List.of(res.getSeatId()));
            }
        }
    }
}
