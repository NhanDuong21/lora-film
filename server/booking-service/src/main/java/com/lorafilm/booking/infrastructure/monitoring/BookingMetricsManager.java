package com.lorafilm.booking.infrastructure.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.TimeUnit;

@Component
public class BookingMetricsManager {

    private final MeterRegistry meterRegistry;

    // API Metrics
    private final Counter apiRequestCounter;
    private final Counter apiErrorCounter;
    private final Timer apiLatencyTimer;

    // Booking Metrics
    private final Counter bookingCreatedCounter;
    private final Counter bookingConfirmedCounter;
    private final Counter bookingCancelledCounter;
    private final Counter bookingExpiredCounter;

    // Seat Metrics
    private final Counter seatReservedCounter;
    private final Counter seatReleaseCounter;
    private final Counter seatExpiredCounter;

    // Payment Metrics
    private final Counter paymentSuccessCounter;
    private final Counter paymentFailedCounter;

    // Outbox Metrics
    private final Counter outboxCreatedCounter;
    private final Counter outboxPublishedCounter;
    private final Counter outboxFailedCounter;

    // Inbox Metrics
    private final Counter inboxReceivedCounter;
    private final Counter inboxProcessedCounter;
    private final Counter inboxFailedCounter;

    // Retry Metrics
    private final Counter retryCreatedCounter;
    private final Counter retrySuccessCounter;
    private final Counter retryFailedCounter;

    // Redis Metrics
    private final Counter redisLockSuccessCounter;
    private final Counter redisLockFailedCounter;

    // Promotion reconciliation metrics
    private final Counter promotionReconciliationMismatchCounter;
    private final AtomicLong promotionReconciliationMismatch = new AtomicLong();

    public BookingMetricsManager(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // API Metrics
        this.apiRequestCounter = Counter.builder("booking_api_request_total")
                .description("Total number of API requests to Booking Service")
                .register(meterRegistry);
        this.apiErrorCounter = Counter.builder("booking_api_error_total")
                .description("Total number of failed API requests to Booking Service")
                .register(meterRegistry);
        this.apiLatencyTimer = Timer.builder("booking_api_latency")
                .description("Latency of API requests to Booking Service")
                .register(meterRegistry);

        // Booking Metrics
        this.bookingCreatedCounter = Counter.builder("booking_created_total")
                .description("Total bookings created")
                .register(meterRegistry);
        this.bookingConfirmedCounter = Counter.builder("booking_confirmed_total")
                .description("Total bookings confirmed")
                .register(meterRegistry);
        this.bookingCancelledCounter = Counter.builder("booking_cancelled_total")
                .description("Total bookings cancelled")
                .register(meterRegistry);
        this.bookingExpiredCounter = Counter.builder("booking_expired_total")
                .description("Total bookings expired")
                .register(meterRegistry);

        // Seat Metrics
        this.seatReservedCounter = Counter.builder("seat_reserved_total")
                .description("Total seats reserved")
                .register(meterRegistry);
        this.seatReleaseCounter = Counter.builder("seat_release_total")
                .description("Total seats released")
                .register(meterRegistry);
        this.seatExpiredCounter = Counter.builder("seat_expired_total")
                .description("Total seat reservations expired")
                .register(meterRegistry);

        // Payment Metrics
        this.paymentSuccessCounter = Counter.builder("payment_success_total")
                .description("Total successful payments")
                .register(meterRegistry);
        this.paymentFailedCounter = Counter.builder("payment_failed_total")
                .description("Total failed payments")
                .register(meterRegistry);

        // Outbox Metrics
        this.outboxCreatedCounter = Counter.builder("outbox_event_created_total")
                .description("Total outbox events created")
                .register(meterRegistry);
        this.outboxPublishedCounter = Counter.builder("outbox_event_published_total")
                .description("Total outbox events published")
                .register(meterRegistry);
        this.outboxFailedCounter = Counter.builder("outbox_event_failed_total")
                .description("Total outbox events failed")
                .register(meterRegistry);

        // Inbox Metrics
        this.inboxReceivedCounter = Counter.builder("inbox_event_received_total")
                .description("Total inbox events received")
                .register(meterRegistry);
        this.inboxProcessedCounter = Counter.builder("inbox_event_processed_total")
                .description("Total inbox events processed")
                .register(meterRegistry);
        this.inboxFailedCounter = Counter.builder("inbox_event_failed_total")
                .description("Total inbox events failed")
                .register(meterRegistry);

        // Retry Metrics
        this.retryCreatedCounter = Counter.builder("retry_task_created_total")
                .description("Total retry tasks created")
                .register(meterRegistry);
        this.retrySuccessCounter = Counter.builder("retry_task_success_total")
                .description("Total retry tasks successfully completed")
                .register(meterRegistry);
        this.retryFailedCounter = Counter.builder("retry_task_failed_total")
                .description("Total retry tasks failed")
                .register(meterRegistry);

        // Redis Metrics
        this.redisLockSuccessCounter = Counter.builder("redis_lock_success_total")
                .description("Total successful redis lock acquisitions")
                .register(meterRegistry);
        this.redisLockFailedCounter = Counter.builder("redis_lock_failed_total")
                .description("Total failed redis lock acquisitions")
                .register(meterRegistry);

        this.promotionReconciliationMismatchCounter = Counter.builder(
                        "booking_promotion_reconciliation_mismatch_total")
                .description("Promotion reconciliation tasks that transitioned to mismatch")
                .register(meterRegistry);
        Gauge.builder("booking_promotion_reconciliation_mismatch",
                        promotionReconciliationMismatch, AtomicLong::doubleValue)
                .description("Current unresolved promotion reconciliation mismatches")
                .register(meterRegistry);
    }

    // Helper methods to increment / record metrics
    public void incrementApiRequest() { apiRequestCounter.increment(); }
    public void incrementApiError() { apiErrorCounter.increment(); }
    public void recordApiLatency(long durationMs) { apiLatencyTimer.record(durationMs, TimeUnit.MILLISECONDS); }

    public void incrementBookingCreated() { bookingCreatedCounter.increment(); }
    public void incrementBookingConfirmed() { bookingConfirmedCounter.increment(); }
    public void incrementBookingCancelled() { bookingCancelledCounter.increment(); }
    public void incrementBookingExpired() { bookingExpiredCounter.increment(); }

    public void incrementSeatReserved(int count) { seatReservedCounter.increment(count); }
    public void incrementSeatRelease(int count) { seatReleaseCounter.increment(count); }
    public void incrementSeatExpired(int count) { seatExpiredCounter.increment(count); }

    public void incrementPaymentSuccess() { paymentSuccessCounter.increment(); }
    public void incrementPaymentFailed() { paymentFailedCounter.increment(); }

    public void incrementOutboxCreated() { outboxCreatedCounter.increment(); }
    public void incrementOutboxPublished() { outboxPublishedCounter.increment(); }
    public void incrementOutboxFailed() { outboxFailedCounter.increment(); }

    public void incrementInboxReceived() { inboxReceivedCounter.increment(); }
    public void incrementInboxProcessed() { inboxProcessedCounter.increment(); }
    public void incrementInboxFailed() { inboxFailedCounter.increment(); }

    public void incrementRetryCreated() { retryCreatedCounter.increment(); }
    public void incrementRetrySuccess() { retrySuccessCounter.increment(); }
    public void incrementRetryFailed() { retryFailedCounter.increment(); }

    public void incrementRedisLockSuccess() { redisLockSuccessCounter.increment(); }
    public void incrementRedisLockFailed() { redisLockFailedCounter.increment(); }

    public void incrementPromotionReconciliationMismatch() {
        promotionReconciliationMismatchCounter.increment();
    }

    public void updatePromotionReconciliationMismatch(long count) {
        promotionReconciliationMismatch.set(count);
    }

    // Getter methods for monitoring summary
    public double getApiRequestCount() { return apiRequestCounter.count(); }
    public double getApiErrorCount() { return apiErrorCounter.count(); }
    public double getApiLatencyMean() { return apiLatencyTimer.mean(TimeUnit.MILLISECONDS); }

    public double getBookingCreated() { return bookingCreatedCounter.count(); }
    public double getBookingConfirmed() { return bookingConfirmedCounter.count(); }
    public double getBookingCancelled() { return bookingCancelledCounter.count(); }
    public double getBookingExpired() { return bookingExpiredCounter.count(); }

    public double getSeatReserved() { return seatReservedCounter.count(); }
    public double getSeatRelease() { return seatReleaseCounter.count(); }
    public double getSeatExpired() { return seatExpiredCounter.count(); }

    public double getPaymentSuccess() { return paymentSuccessCounter.count(); }
    public double getPaymentFailed() { return paymentFailedCounter.count(); }

    public double getOutboxCreated() { return outboxCreatedCounter.count(); }
    public double getOutboxPublished() { return outboxPublishedCounter.count(); }
    public double getOutboxFailed() { return outboxFailedCounter.count(); }

    public double getInboxReceived() { return inboxReceivedCounter.count(); }
    public double getInboxProcessed() { return inboxProcessedCounter.count(); }
    public double getInboxFailed() { return inboxFailedCounter.count(); }

    public double getRetryCreated() { return retryCreatedCounter.count(); }
    public double getRetrySuccess() { return retrySuccessCounter.count(); }
    public double getRetryFailed() { return retryFailedCounter.count(); }

    public double getRedisLockSuccess() { return redisLockSuccessCounter.count(); }
    public double getRedisLockFailed() { return redisLockFailedCounter.count(); }
    public double getPromotionReconciliationMismatchTransitions() {
        return promotionReconciliationMismatchCounter.count();
    }
    public long getPromotionReconciliationMismatch() {
        return promotionReconciliationMismatch.get();
    }
}
