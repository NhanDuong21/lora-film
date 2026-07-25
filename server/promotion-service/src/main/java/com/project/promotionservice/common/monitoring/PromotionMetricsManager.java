package com.project.promotionservice.common.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class PromotionMetricsManager {

    private final Counter apiRequestCounter;
    private final Counter apiErrorCounter;
    private final Timer apiLatencyTimer;

    public PromotionMetricsManager(MeterRegistry meterRegistry) {
        this.apiRequestCounter = Counter.builder("promotion_api_request_total")
                .description("Total number of API requests to Promotion Service")
                .register(meterRegistry);
        this.apiErrorCounter = Counter.builder("promotion_api_error_total")
                .description("Total number of failed API requests to Promotion Service")
                .register(meterRegistry);
        this.apiLatencyTimer = Timer.builder("promotion_api_latency")
                .description("Latency of API requests to Promotion Service")
                .register(meterRegistry);
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
}
