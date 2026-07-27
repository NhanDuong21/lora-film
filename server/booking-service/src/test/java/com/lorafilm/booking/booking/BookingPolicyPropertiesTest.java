package com.lorafilm.booking.booking;

import com.lorafilm.booking.booking.service.impl.BookingServiceImpl;
import com.lorafilm.booking.config.BookingPolicyProperties;
import com.lorafilm.booking.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookingPolicyPropertiesTest {

    @Test
    void defaultsAreSourceGroundedAndExplicit() {
        BookingPolicyProperties properties = new BookingPolicyProperties();

        assertEquals(8, properties.getMaxSeatsPerBooking());
        assertEquals(900, properties.getHoldDurationSeconds());
        assertEquals(30, properties.getCreationLockTtlSeconds());
    }

    @Test
    void deadlineUsesConfiguredDurationAndShowtimeBoundary() {
        BookingPolicyProperties properties = new BookingPolicyProperties();
        properties.setHoldDurationSeconds(900);
        BookingServiceImpl service = new BookingServiceImpl(
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, properties, null);

        Instant now = Instant.parse("2026-07-26T10:00:00Z");
        Instant farShowtime = now.plusSeconds(3600);
        Instant nearShowtime = now.plusSeconds(600);

        Instant farDeadline = ReflectionTestUtils.invokeMethod(
                service, "calculateDeadline", now, farShowtime, null);
        Instant nearDeadline = ReflectionTestUtils.invokeMethod(
                service, "calculateDeadline", now, nearShowtime, null);

        assertEquals(now.plusSeconds(900), farDeadline);
        assertEquals(nearShowtime, nearDeadline);
        assertEquals(900, Duration.between(now, farDeadline).toSeconds());
        assertThrows(BusinessException.class, () -> ReflectionTestUtils.invokeMethod(
                service, "calculateDeadline", now, now, null));
    }

    @Test
    void existingCompatibilityDeadlineCanOnlyShortenTheConfiguredWindow() {
        BookingPolicyProperties properties = new BookingPolicyProperties();
        BookingServiceImpl service = new BookingServiceImpl(
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, properties, null);

        Instant now = Instant.parse("2026-07-26T10:00:00Z");
        Instant showtime = now.plusSeconds(3600);
        Instant existingDeadline = now.plusSeconds(300);

        Instant deadline = ReflectionTestUtils.invokeMethod(
                service, "calculateDeadline", now, showtime, existingDeadline);

        assertEquals(existingDeadline, deadline);
    }
}
