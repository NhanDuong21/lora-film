package com.lorafilm.booking.payment.scheduler;

import com.lorafilm.booking.infrastructure.entity.BookingRetryTask;
import com.lorafilm.booking.infrastructure.entity.BookingDeadLetterEvent;
import com.lorafilm.booking.infrastructure.enums.RetryTaskStatus;
import com.lorafilm.booking.infrastructure.enums.RetryTaskType;
import com.lorafilm.booking.infrastructure.repository.BookingRetryTaskRepository;
import com.lorafilm.booking.infrastructure.repository.BookingDeadLetterEventRepository;
import com.lorafilm.booking.payment.event.PaymentEventConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.lorafilm.booking.infrastructure.lock.SchedulerLock;

@Component
public class RetryTaskScheduler {

    private static final Logger log = LoggerFactory.getLogger(RetryTaskScheduler.class);
    private static final int BATCH_SIZE = 20;

    private final BookingRetryTaskRepository retryTaskRepository;
    private final PaymentEventConsumer paymentEventConsumer;
    private final BookingDeadLetterEventRepository deadLetterEventRepository;

    public RetryTaskScheduler(
            BookingRetryTaskRepository retryTaskRepository,
            PaymentEventConsumer paymentEventConsumer,
            BookingDeadLetterEventRepository deadLetterEventRepository) {
        this.retryTaskRepository = retryTaskRepository;
        this.paymentEventConsumer = paymentEventConsumer;
        this.deadLetterEventRepository = deadLetterEventRepository;
    }

    @Scheduled(fixedDelay = 10000) // Poll database every 10 seconds
    @SchedulerLock(name = "RetryTaskScheduler", lockAtMostForSeconds = 8)
    public void processRetryTasks() {
        Instant now = Instant.now();
        List<BookingRetryTask> pendingTasks = retryTaskRepository.findByStatusAndNextRetryAtBefore(
                RetryTaskStatus.PENDING, now, PageRequest.of(0, BATCH_SIZE));

        if (pendingTasks.isEmpty()) {
            return;
        }

        log.info("Processing {} pending retry tasks...", pendingTasks.size());

        for (BookingRetryTask task : pendingTasks) {
            task.setStatus(RetryTaskStatus.RUNNING);
            retryTaskRepository.saveAndFlush(task);

            try {
                if (task.getTaskType() == RetryTaskType.INBOX_PROCESS) {
                    paymentEventConsumer.consume(task.getPayload());
                    task.setStatus(RetryTaskStatus.SUCCESS);
                    log.info("Successfully executed retry task publicId: {}", task.getPublicId());
                } else if (task.getTaskType() == RetryTaskType.OUTBOX_PUBLISH) {
                    log.info("Ignored OUTBOX_PUBLISH task as it is handled by OutboxEventPublisherScheduler");
                    task.setStatus(RetryTaskStatus.SUCCESS);
                } else {
                    task.setErrorCode("NO_RETRY_HANDLER");
                    task.setRetryCount(task.getMaxRetry());
                    task.setLastRetryAt(Instant.now());
                    task.setErrorMessage("No retry handler is registered for task type " + task.getTaskType());
                    moveToDeadLetter(task);
                    continue;
                }
                retryTaskRepository.save(task);

            } catch (Exception e) {
                int count = task.getRetryCount() + 1;
                task.setRetryCount(count);
                task.setLastRetryAt(Instant.now());
                task.setErrorMessage(e.getMessage());

                if (count >= task.getMaxRetry()) {
                    moveToDeadLetter(task);
                } else {
                    task.setStatus(RetryTaskStatus.PENDING);
                    // Exponential backoff: 30s, 60s, 120s...
                    long delaySeconds = (long) Math.pow(2, count) * 15L;
                    task.setNextRetryAt(Instant.now().plusSeconds(delaySeconds));
                    log.warn("Retry task publicId: {} failed (attempt {}/{}). Next retry at: {}",
                            task.getPublicId(), count, task.getMaxRetry(), task.getNextRetryAt());
                }
                retryTaskRepository.save(task);
            }
        }
    }

    private void moveToDeadLetter(BookingRetryTask task) {
        task.setStatus(RetryTaskStatus.DEAD_LETTER);
        log.error("Retry task publicId: {} cannot be retried. Moved to DEAD_LETTER.",
                task.getPublicId());

        BookingDeadLetterEvent dlEvent = new BookingDeadLetterEvent();
        dlEvent.setPublicId(UUID.randomUUID().toString());
        dlEvent.setEventId(task.getPublicId());
        dlEvent.setSourceTable("booking_retry_tasks");
        dlEvent.setAggregateType(task.getReferenceType());
        dlEvent.setAggregateId(task.getReferenceId());
        dlEvent.setEventType(task.getTaskType() != null ? task.getTaskType().name() : "UNKNOWN");
        dlEvent.setPayload(task.getPayload());
        dlEvent.setRetryCount(task.getRetryCount());
        dlEvent.setErrorCode(task.getErrorCode());
        dlEvent.setErrorMessage(task.getErrorMessage());
        dlEvent.setMovedAt(Instant.now());
        deadLetterEventRepository.save(dlEvent);
        retryTaskRepository.save(task);
    }
}
