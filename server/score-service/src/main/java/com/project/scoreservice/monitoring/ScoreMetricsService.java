package com.project.scoreservice.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class ScoreMetricsService {

    private final Counter pointsEarnedCounter;
    private final Counter pointsRedeemedCounter;
    private final Counter pointsExpiredCounter;
    private final Counter outboxPublishedCounter;
    private final Counter outboxFailedCounter;
    private final Counter tierUpgradeCounter;

    public ScoreMetricsService(MeterRegistry meterRegistry) {
        this.pointsEarnedCounter = meterRegistry.counter("score.points.earned.total");
        this.pointsRedeemedCounter = meterRegistry.counter("score.points.redeemed.total");
        this.pointsExpiredCounter = meterRegistry.counter("score.points.expired.total");
        this.outboxPublishedCounter = meterRegistry.counter("score.outbox.published.total");
        this.outboxFailedCounter = meterRegistry.counter("score.outbox.failed.total");
        this.tierUpgradeCounter = meterRegistry.counter("score.tier.upgrades.total");
    }

    public void recordPointsEarned(int points) {
        if (points > 0) {
            pointsEarnedCounter.increment(points);
        }
    }

    public void recordPointsRedeemed(int points) {
        if (points > 0) {
            pointsRedeemedCounter.increment(points);
        }
    }

    public void recordPointsExpired(int points) {
        if (points > 0) {
            pointsExpiredCounter.increment(points);
        }
    }

    public void recordOutboxPublished() {
        outboxPublishedCounter.increment();
    }

    public void recordOutboxFailed() {
        outboxFailedCounter.increment();
    }

    public void recordTierUpgrade() {
        tierUpgradeCounter.increment();
    }
}
