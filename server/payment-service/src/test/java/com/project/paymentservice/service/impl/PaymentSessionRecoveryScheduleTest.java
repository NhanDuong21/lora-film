package com.project.paymentservice.service.impl;

import com.project.paymentservice.entity.Payment;
import com.project.paymentservice.provider.PaymentSession;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentSessionRecoveryScheduleTest {

    @Test
    void schedulesNormalRecoveryAtProviderSessionExpiryWhenItComesFirst() {
        Instant bookingExpiry = Instant.parse("2026-07-29T10:15:00Z");
        Instant sessionExpiry = Instant.parse("2026-07-29T10:10:00Z");
        Payment payment = new Payment();
        payment.setBookingExpiresAt(bookingExpiry);
        PaymentSession session = new PaymentSession("ORDER", "SESSION", "URL", sessionExpiry);

        assertEquals(sessionExpiry, PaymentServiceImpl.normalSessionRecoveryAt(payment, session));
    }

    @Test
    void neverSchedulesNormalRecoveryAfterTheOriginalBookingDeadline() {
        Instant bookingExpiry = Instant.parse("2026-07-29T10:05:00Z");
        Instant sessionExpiry = Instant.parse("2026-07-29T10:10:00Z");
        Payment payment = new Payment();
        payment.setBookingExpiresAt(bookingExpiry);
        PaymentSession session = new PaymentSession("ORDER", "SESSION", "URL", sessionExpiry);

        assertEquals(bookingExpiry, PaymentServiceImpl.normalSessionRecoveryAt(payment, session));
    }
}
