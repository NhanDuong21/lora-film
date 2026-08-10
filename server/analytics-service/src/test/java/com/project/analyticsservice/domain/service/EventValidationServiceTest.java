package com.project.analyticsservice.domain.service;

import com.project.analyticsservice.exception.NonRetryableAnalyticsEventException;
import com.project.analyticsservice.kafka.event.PaymentSucceededEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventValidationServiceTest {
    private final EventValidationService service = new EventValidationService();

    @Test
    void acceptsPaymentWhenFinancialInvariantMatches() {
        assertDoesNotThrow(() -> service.validate(payment("325000", "170000", "155000", "0", "325000")));
    }

    @Test
    void rejectsPaymentWhenProducerBreaksFinancialInvariant() {
        assertThrows(NonRetryableAnalyticsEventException.class,
                () -> service.validate(payment("325000", "170000", "155000", "10000", "325000")));
    }

    private PaymentSucceededEvent payment(
            String amount, String ticket, String food, String discount, String total) {
        return new PaymentSucceededEvent(
                "event-1", "1.0", "payment-1", "booking-1", "VNPAY",
                new BigDecimal(amount), "VND", Instant.parse("2026-07-27T04:15:30Z"),
                9L, "movie-9", "Movie", "showtime-1", "cinema-1", "Cinema",
                "room-1", 2, 100, new BigDecimal(ticket), new BigDecimal(food),
                new BigDecimal(discount), new BigDecimal(total),
                "user-1", "promo-1", "Promo", "GOLD", "VNPAY", null, null);
    }
}
