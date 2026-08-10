package com.lorafilm.booking.booking;

import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.common.exception.InvalidBookingStatusException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookingAggregateTest {

    @Test
    void shouldCreatePendingBookingAndCalculateFinalAmount() {
        Booking booking = createBooking(Instant.now().plusSeconds(900));

        assertEquals(BookingStatus.PENDING_PAYMENT, booking.getBookingStatus());
        assertEquals(new BigDecimal("210000.00"), booking.getFinalAmount());
        assertNotNull(booking.getPublicId());
    }

    @Test
    void shouldFollowConfirmedToCompletedLifecycle() {
        Booking booking = createBooking(Instant.now().plusSeconds(900));
        Instant confirmedAt = Instant.now();
        Instant completedAt = confirmedAt.plusSeconds(7200);

        booking.changeStatus(BookingStatus.CONFIRMED, confirmedAt);
        booking.changeStatus(BookingStatus.COMPLETED, completedAt);

        assertEquals(BookingStatus.COMPLETED, booking.getBookingStatus());
        assertEquals(confirmedAt, booking.getConfirmedAt());
        assertEquals(completedAt, booking.getCompletedAt());
    }

    @Test
    void shouldRejectTransitionFromCancelledToConfirmed() {
        Booking booking = createBooking(Instant.now().plusSeconds(900));
        booking.cancel("USER_CANCEL", "Changed plans", Instant.now());

        assertThrows(
                InvalidBookingStatusException.class,
                () -> booking.changeStatus(BookingStatus.CONFIRMED, Instant.now()));
    }

    @Test
    void shouldRejectConfirmationAfterPaymentDeadline() {
        Booking booking = createBooking(Instant.now().minusSeconds(1));

        assertThrows(
                InvalidBookingStatusException.class,
                () -> booking.changeStatus(BookingStatus.CONFIRMED, Instant.now()));
    }

    @Test
    void shouldApplyScoreDiscountBeforeLockingAmount() {
        Booking booking = createBooking(Instant.now().plusSeconds(900));

        booking.applyScoreRedemption(
                50,
                new BigDecimal("50000"),
                "HOLD-POINTS-1");
        booking.lockAmount(Instant.now());

        assertEquals(50, booking.getScorePointsUsed());
        assertEquals(new BigDecimal("50000.00"), booking.getScoreDiscount());
        assertEquals(new BigDecimal("160000.00"), booking.getFinalAmount());
        assertEquals("HOLD-POINTS-1", booking.getScoreHoldCode());
    }

    @Test
    void shouldOnlyExpireAfterPaymentDeadline() {
        Booking activeBooking = createBooking(Instant.now().plusSeconds(900));
        assertThrows(
                InvalidBookingStatusException.class,
                () -> activeBooking.changeStatus(BookingStatus.EXPIRED, Instant.now()));

        Booking overdueBooking = createBooking(Instant.now().minusSeconds(1));
        overdueBooking.changeStatus(BookingStatus.EXPIRED, Instant.now());
        assertEquals(BookingStatus.EXPIRED, overdueBooking.getBookingStatus());
    }

    private Booking createBooking(Instant expiresAt) {
        return Booking.create(
                UUID.randomUUID().toString(),
                "LORAFILM-20260720-000001",
                15L,
                1001L,
                101L,
                201L,
                301L,
                new BigDecimal("200000"),
                BigDecimal.ZERO,
                new BigDecimal("20000"),
                BigDecimal.ZERO,
                new BigDecimal("10000"),
                BigDecimal.ZERO,
                "VND",
                expiresAt,
                null);
    }
}
