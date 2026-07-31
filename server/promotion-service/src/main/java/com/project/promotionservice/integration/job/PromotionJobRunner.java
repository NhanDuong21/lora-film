package com.project.promotionservice.integration.job;

import com.project.promotionservice.configuration.domain.ConfigurationService;
import com.project.promotionservice.integration.inbox.IntegrationEventService;
import com.project.promotionservice.integration.outbox.OutboxStatus;
import com.project.promotionservice.integration.outbox.OutboxEventPublisherScheduler;
import com.project.promotionservice.integration.outbox.PromotionOutboxEventRepository;
import com.project.promotionservice.reservation.service.PromotionReservationService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PromotionJobRunner {
    private static final List<String> JOBS = List.of(
            "campaigns-activate", "campaigns-expire", "coupons-activate", "coupons-expire", "vouchers-expire",
            "reservations-expire", "outbox-publish", "outbox-retry",
            "integration-retry", "cache-refresh");
    private final JobExecutionService executions;
    private final PromotionLifecycleService lifecycle;
    private final PromotionReservationService reservations;
    private final OutboxEventPublisherScheduler outboxPublisher;
    private final PromotionOutboxEventRepository outbox;
    private final IntegrationEventService integration;
    private final ConfigurationService configurations;

    public PromotionJobRunner(JobExecutionService executions,
                              PromotionLifecycleService lifecycle,
                              PromotionReservationService reservations,
                              OutboxEventPublisherScheduler outboxPublisher,
                              PromotionOutboxEventRepository outbox,
                              IntegrationEventService integration,
                              ConfigurationService configurations) {
        this.executions = executions; this.lifecycle = lifecycle; this.reservations = reservations;
        this.outboxPublisher = outboxPublisher; this.outbox = outbox;
        this.integration = integration; this.configurations = configurations;
    }

    public int run(String jobName, String trigger, String actor) {
        String name = normalize(jobName);
        return executions.run(name, trigger, actor, () -> switch (name) {
            case "campaigns-activate" -> lifecycle.activateCampaigns(actor);
            case "campaigns-expire" -> lifecycle.expireCampaigns(actor);
            case "coupons-activate" -> lifecycle.activateCoupons(actor);
            case "coupons-expire" -> lifecycle.expireCoupons(actor);
            case "vouchers-expire" -> lifecycle.expireVouchers(actor);
            case "reservations-expire" -> reservations.expireDueReservations(actor);
            case "outbox-publish" -> { outboxPublisher.publishPendingEvents(); yield 0; }
            case "outbox-retry" -> retryOutbox();
            case "integration-retry" -> integration.retryDue();
            case "cache-refresh" -> configurations.refreshCache();
            default -> throw new IllegalArgumentException("Unknown scheduler job: " + jobName);
        });
    }

    public Map<String, List<SchedulerJobExecution>> recentAll() {
        return JOBS.stream().collect(java.util.stream.Collectors.toMap(
                name -> name, executions::recent, (a, b) -> a, java.util.LinkedHashMap::new));
    }

    private int retryOutbox() {
        var events = outbox.findByPublishStatusIn(
                List.of(OutboxStatus.FAILED, OutboxStatus.DEAD_LETTER),
                org.springframework.data.domain.PageRequest.of(0, 100));
        for (var event : events) {
            event.setPublishStatus(OutboxStatus.PENDING);
            event.setNextRetryAt(java.time.Instant.now());
            event.setErrorMessage(null);
            event.setProcessingOwner(null);
            event.setProcessingStartedAt(null);
            outbox.save(event);
        }
        return events.size();
    }

    private String normalize(String jobName) {
        return jobName.toLowerCase().replace('_', '-');
    }
}
