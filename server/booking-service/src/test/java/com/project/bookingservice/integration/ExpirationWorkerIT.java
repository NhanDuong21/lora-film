package com.project.bookingservice.integration;

import com.project.bookingservice.entity.Booking;
import com.project.bookingservice.entity.SeatReservation;
import com.project.bookingservice.enumtype.BookingStatus;
import com.project.bookingservice.enumtype.ReservationStatus;
import com.project.bookingservice.repository.BookingRepository;
import com.project.bookingservice.repository.SeatReservationRepository;
import com.project.bookingservice.service.lock.SeatLockManager;
import com.project.bookingservice.worker.BookingExpirationWorker;
import com.project.bookingservice.worker.ReservationExpirationWorker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("local")
public class ExpirationWorkerIT {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private SeatReservationRepository seatReservationRepository;

    @Autowired
    private ReservationExpirationWorker reservationExpirationWorker;

    @Autowired
    private BookingExpirationWorker bookingExpirationWorker;

    @MockBean
    private SeatLockManager seatLockManager;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        seatReservationRepository.deleteAll();
        doNothing().when(seatLockManager).evictLocks(any(), anyList());
    }

    @Test
    void testReservationExpirationWorker() {
        SeatReservation res = new SeatReservation();
        res.setShowtimeId(1L);
        res.setSeatId(10L);
        res.setUserId(100L);
        res.setStatus(ReservationStatus.HELD);
        res.setExpiresAt(LocalDateTime.now().minusMinutes(10));
        res = seatReservationRepository.saveAndFlush(res);

        reservationExpirationWorker.processExpiredReservations();

        Optional<SeatReservation> updated = seatReservationRepository.findById(res.getId());
        assertThat(updated.isPresent()).isTrue();
        assertThat(updated.get().getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        
        verify(seatLockManager, times(1)).evictLocks(eq(1L), anyList());
    }

    @Test
    void testBookingExpirationWorker() {
        Booking booking = new Booking();
        booking.setBookingCode("B-1234");
        booking.setUserId(100L);
        booking.setShowtimeId(1L);
        booking.setTotalAmount(new BigDecimal("150.00"));
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setExpiresAt(LocalDateTime.now().minusMinutes(20));
        booking.setVersion(0);
        booking = bookingRepository.saveAndFlush(booking);

        SeatReservation res = new SeatReservation();
        res.setShowtimeId(1L);
        res.setSeatId(10L);
        res.setUserId(100L);
        res.setBookingId(booking.getId());
        res.setStatus(ReservationStatus.CONVERTED);
        res.setExpiresAt(LocalDateTime.now().minusMinutes(20));
        res = seatReservationRepository.saveAndFlush(res);

        bookingExpirationWorker.processExpiredBookings();

        Optional<Booking> updatedBooking = bookingRepository.findById(booking.getId());
        assertThat(updatedBooking.isPresent()).isTrue();
        assertThat(updatedBooking.get().getStatus()).isEqualTo(BookingStatus.EXPIRED);

        Optional<SeatReservation> updatedRes = seatReservationRepository.findById(res.getId());
        assertThat(updatedRes.isPresent()).isTrue();
        assertThat(updatedRes.get().getStatus()).isEqualTo(ReservationStatus.EXPIRED);

        verify(seatLockManager, times(1)).evictLocks(eq(1L), anyList());
    }
}
