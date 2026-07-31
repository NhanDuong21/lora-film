package com.project.analyticsservice.scheduler;

import com.project.analyticsservice.application.KpiPipelineApplicationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class KpiCalculationScheduler {
    private final KpiPipelineApplicationService pipeline;
    private final ZoneId businessZone;
    private final int lookbackDays;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public KpiCalculationScheduler(
            KpiPipelineApplicationService pipeline,
            @Value("${analytics.zone-id:Asia/Ho_Chi_Minh}") String zoneId,
            @Value("${analytics.scheduler.lookback-days:3}") int lookbackDays) {
        this.pipeline = pipeline;
        this.businessZone = ZoneId.of(zoneId);
        this.lookbackDays = Math.max(1, Math.min(lookbackDays, 31));
    }

    @Scheduled(
            cron = "${analytics.scheduler.cron:0 10 1 * * *}",
            zone = "${analytics.zone-id:Asia/Ho_Chi_Minh}")
    public void calculateRecentKpis() {
        runGuarded(() -> {
            LocalDate yesterday = LocalDate.now(businessZone).minusDays(1);
            for (int offset = lookbackDays - 1; offset >= 0; offset--) {
                pipeline.calculate(yesterday.minusDays(offset));
            }
        });
    }

    @Scheduled(
            fixedDelayString = "${analytics.scheduler.today-delay-ms:60000}",
            initialDelayString = "${analytics.scheduler.today-initial-delay-ms:10000}")
    public void calculateTodayKpis() {
        runGuarded(() -> pipeline.calculateIfStale(LocalDate.now(businessZone)));
    }

    private void runGuarded(Runnable calculation) {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        try {
            calculation.run();
        } finally {
            running.set(false);
        }
    }
}
