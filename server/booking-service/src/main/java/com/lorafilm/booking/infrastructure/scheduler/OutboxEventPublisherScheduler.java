package com.lorafilm.booking.infrastructure.scheduler;

import com.lorafilm.booking.infrastructure.entity.BookingOutboxEvent;
import com.lorafilm.booking.infrastructure.enums.OutboxStatus;
import com.lorafilm.booking.infrastructure.lock.SchedulerLock;
import com.lorafilm.booking.infrastructure.repository.BookingOutboxEventRepository;
import com.lorafilm.booking.infrastructure.service.BookingEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class OutboxEventPublisherScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisherScheduler.class);
    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRIES = 5;

    private final BookingOutboxEventRepository outboxEventRepository;
    private final BookingEventPublisher eventPublisher;

    public OutboxEventPublisherScheduler(
            BookingOutboxEventRepository outboxEventRepository,
            BookingEventPublisher eventPublisher) {
        this.outboxEventRepository = outboxEventRepository;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedDelay = 5000) // Poll database every 5 seconds
    @SchedulerLock(name = "OutboxEventPublisherScheduler", lockAtMostForSeconds = 8)
    public void publishPendingEvents() {
        log.debug("OutboxEventPublisherScheduler starting polling...");
        Instant now = Instant.now();
        List<BookingOutboxEvent> pendingEvents = outboxEventRepository.findPendingEvents(
                OutboxStatus.PENDING, now, PageRequest.of(0, BATCH_SIZE));

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Found {} pending outbox events to publish.", pendingEvents.size());

        for (BookingOutboxEvent event : pendingEvents) {
            event.setStatus(OutboxStatus.PROCESSING);
            outboxEventRepository.saveAndFlush(event);

            try {
                eventPublisher.publish(event);
                event.setStatus(OutboxStatus.PUBLISHED);
                event.setPublishedAt(Instant.now());
                event.setErrorMessage(null);
                outboxEventRepository.save(event);
                log.info("Successfully published outbox event: {}", event.getEventId());
            } catch (Exception e) {
                int count = event.getRetryCount() + 1;
                event.setRetryCount(count);
                event.setErrorMessage(e.getMessage());
                event.setUpdatedAt(Instant.now());

                if (count >= MAX_RETRIES) {
                    event.setStatus(OutboxStatus.FAILED);
                    log.error("Outbox event ID: {} failed and reached max retries. Marked as FAILED.", event.getEventId());
                } else {
                    event.setStatus(OutboxStatus.PENDING);
                    // Exponential backoff: 10s, 20s, 40s...
                    long delaySeconds = (long) Math.pow(2, count) * 5L;
                    event.setNextRetryAt(Instant.now().plusSeconds(delaySeconds));
                    log.warn("Outbox event ID: {} failed to publish (attempt {}/{}). Next retry at: {}",
                            event.getEventId(), count, MAX_RETRIES, event.getNextRetryAt());
                }
                outboxEventRepository.save(event);
            }
        }
    }
}
