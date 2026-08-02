package com.project.promotionservice.common.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;

@Component
public class PromotionMetricsManager {

    private final MeterRegistry meterRegistry;
    private final Counter apiRequestCounter;
    private final Counter apiErrorCounter;
    private final Timer apiLatencyTimer;
    private final AtomicLong expirationBacklog = new AtomicLong();
    private final AtomicLong oldestExpiredAgeSeconds = new AtomicLong();
    private final AtomicLong reversalCount = new AtomicLong();
    private final AtomicLong reversalsLastHour = new AtomicLong();
    private final AtomicReference<BigDecimal> budgetReserved =
            new AtomicReference<>(BigDecimal.ZERO);
    private final AtomicReference<BigDecimal> budgetExposure =
            new AtomicReference<>(BigDecimal.ZERO);
    private final AtomicLong campaignsAtExposureThreshold = new AtomicLong();
    private final AtomicLong activeAlertCount = new AtomicLong();

    public PromotionMetricsManager(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.apiRequestCounter = Counter.builder("promotion_api_request_total")
                .description("Total number of API requests to Promotion Service")
                .register(meterRegistry);
        this.apiErrorCounter = Counter.builder("promotion_api_error_total")
                .description("Total number of failed API requests to Promotion Service")
                .register(meterRegistry);
        this.apiLatencyTimer = Timer.builder("promotion_api_latency")
                .description("Latency of API requests to Promotion Service")
                .register(meterRegistry);
        registerOperationsGauges(meterRegistry);
    }

    public void incrementApiRequest() {
        apiRequestCounter.increment();
    }

    public void incrementApiError() {
        apiErrorCounter.increment();
    }

    public void recordApiLatency(long durationMs) {
        apiLatencyTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void incrementOutboxDelivery(String outcome) {
        Counter.builder("promotion_outbox_delivery_total")
                .description("Promotion outbox delivery attempts")
                .tag("outcome", outcome)
                .register(meterRegistry)
                .increment();
    }

    public void incrementReservationExpiration(String outcome) {
        Counter.builder("promotion_reservation_expiration_total")
                .description("Promotion reservation expiration attempts")
                .tag("outcome", outcome)
                .register(meterRegistry)
                .increment();
    }

    public void updateOperations(PromotionOperationsSummary summary) {
        expirationBacklog.set(summary.expirationBacklog());
        oldestExpiredAgeSeconds.set(summary.oldestExpiredAgeSeconds());
        reversalCount.set(summary.reversalCount());
        reversalsLastHour.set(summary.reversalsLastHour());
        budgetReserved.set(summary.activeBudgetReserved());
        budgetExposure.set(summary.activeBudgetExposure());
        campaignsAtExposureThreshold.set(summary.campaignsAtExposureThreshold());
        activeAlertCount.set(summary.activeAlerts().size());
    }

    private void registerOperationsGauges(MeterRegistry registry) {
        Gauge.builder("promotion_reservation_expiration_backlog", expirationBacklog,
                        AtomicLong::doubleValue)
                .description("Expired ACTIVE promotion reservations awaiting cleanup")
                .register(registry);
        Gauge.builder("promotion_reservation_oldest_expired_age_seconds",
                        oldestExpiredAgeSeconds, AtomicLong::doubleValue)
                .description("Age in seconds of the oldest expired ACTIVE reservation")
                .register(registry);
        Gauge.builder("promotion_reversal_count", reversalCount,
                        AtomicLong::doubleValue)
                .description("Distinct confirmed reservations reversed in persistent ledger")
                .register(registry);
        Gauge.builder("promotion_reversals_last_hour", reversalsLastHour,
                        AtomicLong::doubleValue)
                .description("Distinct promotion reservation reversals in the last hour")
                .register(registry);
        Gauge.builder("promotion_active_budget_reserved", budgetReserved,
                        value -> value.get().doubleValue())
                .description("Budget reserved by active promotion campaigns")
                .register(registry);
        Gauge.builder("promotion_active_budget_exposure", budgetExposure,
                        value -> value.get().doubleValue())
                .description("Used plus reserved budget exposure for active campaigns")
                .register(registry);
        Gauge.builder("promotion_campaigns_at_exposure_threshold",
                        campaignsAtExposureThreshold, AtomicLong::doubleValue)
                .description("Active campaigns at or above configured budget exposure threshold")
                .register(registry);
        Gauge.builder("promotion_operations_active_alerts", activeAlertCount,
                        AtomicLong::doubleValue)
                .description("Active promotion operations alerts")
                .register(registry);
    }
}
