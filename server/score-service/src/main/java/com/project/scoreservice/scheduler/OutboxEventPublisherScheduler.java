package com.project.scoreservice.scheduler;

import com.project.scoreservice.entity.OutboxEvent;
import com.project.scoreservice.enumtype.OutboxStatus;
import com.project.scoreservice.repository.OutboxEventRepository;
import com.project.scoreservice.service.ScoreEventPublisher;
import com.project.scoreservice.monitoring.ScoreMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OutboxEventPublisherScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisherScheduler.class);
    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRIES = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final ScoreEventPublisher eventPublisher;
    private final ScoreMetricsService metricsService;

    public OutboxEventPublisherScheduler(OutboxEventRepository outboxEventRepository,
                                         ScoreEventPublisher eventPublisher,
                                         ScoreMetricsService metricsService) {
        this.outboxEventRepository = outboxEventRepository;
        this.eventPublisher = eventPublisher;
        this.metricsService = metricsService;
    }

    @Scheduled(fixedDelayString = "${score.outbox.poll-interval:5000}")
    @Transactional
    public void publishPendingEvents() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<OutboxEvent> pendingEvents = outboxEventRepository.findAndClaimPendingEvents(now, BATCH_SIZE);

            if (pendingEvents.isEmpty()) {
                return;
            }

            log.info("Found {} pending outbox events to publish in score-service.", pendingEvents.size());

            for (OutboxEvent event : pendingEvents) {
                try {
                    eventPublisher.publish(event);
                    event.setStatus(OutboxStatus.PUBLISHED);
                    event.setPublishedAt(LocalDateTime.now());
                    event.setErrorMessage(null);
                    outboxEventRepository.save(event);
                    metricsService.recordOutboxPublished();
                    log.info("Successfully published outbox event: {} [{}]", event.getEventId(), event.getEventType());
                } catch (Exception e) {
                    int count = event.getRetryCount() + 1;
                    event.setRetryCount(count);
                    event.setErrorMessage(e.getMessage() != null ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 950)) : "Unknown publish error");

                    if (count >= MAX_RETRIES) {
                        event.setStatus(OutboxStatus.FAILED);
                        metricsService.recordOutboxFailed();
                        log.error("Outbox event ID: {} [{}] reached max retries ({}). Marked as FAILED.",
                                event.getEventId(), event.getEventType(), count);
                    } else {
                        event.setStatus(OutboxStatus.PENDING);
                        long delaySeconds = (long) Math.pow(2, count) * 5L;
                        event.setNextRetryAt(LocalDateTime.now().plusSeconds(delaySeconds));
                        log.warn("Outbox event ID: {} [{}] failed to publish (attempt {}/{}). Next retry at: {}",
                                event.getEventId(), event.getEventType(), count, MAX_RETRIES, event.getNextRetryAt());
                    }
                    outboxEventRepository.save(event);
                }
            }
        } catch (Exception e) {
            log.error("Error in OutboxEventPublisherScheduler polling: {}", e.getMessage(), e);
        }
    }
}
