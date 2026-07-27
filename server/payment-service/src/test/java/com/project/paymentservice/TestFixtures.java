package com.project.paymentservice;

import com.project.paymentservice.client.booking.BookingPaymentContext;
import com.project.paymentservice.entity.BookingPaymentGuard;
import com.project.paymentservice.entity.Payment;
import com.project.paymentservice.enumtype.PaymentMethod;
import com.project.paymentservice.enumtype.ProviderCode;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

final class TestFixtures {

    private TestFixtures() {
    }

    static String bookingPublicId(Long bookingId) {
        String source = "booking:" + (bookingId == null ? UUID.randomUUID() : bookingId);
        return UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8)).toString();
    }

    static Payment complete(Payment payment) {
        if (payment.getPublicId() == null) {
            payment.setPublicId(UUID.randomUUID().toString());
        }
        if (payment.getBookingPublicId() == null) {
            payment.setBookingPublicId(bookingPublicId(payment.getBookingId()));
        }
        if (payment.getBookingAmountLockedAt() == null) {
            payment.setBookingAmountLockedAt(Instant.now().minusSeconds(30));
        }
        if (payment.getExpiresAt() == null) {
            payment.setExpiresAt(Instant.now().plusSeconds(900));
        }
        if (payment.getProviderCode() == null) {
            payment.setProviderCode(providerFor(payment.getPaymentMethod()));
        }
        return payment;
    }

    static BookingPaymentContext complete(BookingPaymentContext context) {
        if (context.getBookingPublicId() == null) {
            context.setBookingPublicId(bookingPublicId(context.getBookingId()));
        }
        if (context.getPayable() == null) {
            context.setPayable(true);
        }
        if (context.getAmountLockedAt() == null) {
            context.setAmountLockedAt(Instant.now().minusSeconds(30));
        }
        if (context.getExpiresAt() == null) {
            context.setExpiresAt(Instant.now().plusSeconds(900));
        }
        BookingPaymentContext.AnalyticsSnapshotData snapshot = context.getAnalyticsSnapshot();
        if (snapshot == null) {
            snapshot = new BookingPaymentContext.AnalyticsSnapshotData();
            snapshot.setMovieId(1L);
            snapshot.setMovieTitle("Test Movie");
            snapshot.setTicketCount(1);
            context.setAnalyticsSnapshot(snapshot);
        }
        if (snapshot.getMovieTitle() == null) {
            snapshot.setMovieTitle("Test Movie");
        }
        if (snapshot.getTicketCount() == null) {
            snapshot.setTicketCount(1);
        }
        if (snapshot.getTicketAmount() == null) {
            snapshot.setTicketAmount(context.getAmount());
        }
        if (snapshot.getFoodAmount() == null) {
            snapshot.setFoodAmount(BigDecimal.ZERO);
        }
        if (snapshot.getDiscountAmount() == null) {
            snapshot.setDiscountAmount(BigDecimal.ZERO);
        }
        if (snapshot.getTotalAmount() == null) {
            snapshot.setTotalAmount(context.getAmount());
        }
        if (snapshot.getCurrency() == null) {
            snapshot.setCurrency(context.getCurrency());
        }
        return context;
    }

    static BookingPaymentGuard guard(Payment payment) {
        BookingPaymentGuard guard = new BookingPaymentGuard();
        guard.setBookingPublicId(payment.getBookingPublicId());
        guard.setBookingId(payment.getBookingId());
        guard.setActivePaymentId(payment.getId());
        guard.setNextAttemptNumber(
                payment.getAttemptNumber() == null ? 2 : payment.getAttemptNumber() + 1);
        return guard;
    }

    private static ProviderCode providerFor(PaymentMethod method) {
        if (method == PaymentMethod.CASH) {
            return ProviderCode.CASH;
        }
        if (method == PaymentMethod.MOMO) {
            return ProviderCode.MOMO;
        }
        if (method == PaymentMethod.VNPAY) {
            return ProviderCode.VNPAY;
        }
        return ProviderCode.MOCK;
    }
}
