package com.lorafilm.booking.payment.scheduler;

import com.lorafilm.booking.infrastructure.entity.BookingRetryTask;
import com.lorafilm.booking.infrastructure.enums.RetryTaskStatus;
import com.lorafilm.booking.infrastructure.enums.RetryTaskType;
import com.lorafilm.booking.infrastructure.repository.BookingRetryTaskRepository;
import com.lorafilm.booking.payment.event.PaymentEventConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class RetryTaskScheduler {

    private static final Logger log = LoggerFactory.getLogger(RetryTaskScheduler.class);
    private static final int BATCH_SIZE = 20;

    private final BookingRetryTaskRepository retryTaskRepository;
    private final PaymentEventConsumer paymentEventConsumer;

    public RetryTaskScheduler(
            BookingRetryTaskRepository retryTaskRepository,
            PaymentEventConsumer paymentEventConsumer) {
        this.retryTaskRepository = retryTaskRepository;
        this.paymentEventConsumer = paymentEventConsumer;
    }

    @Scheduled(fixedDelay = 10000) // Poll database every 10 seconds
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
                } else {
                    task.setStatus(RetryTaskStatus.SUCCESS);
                }
                retryTaskRepository.save(task);

            } catch (Exception e) {
                int count = task.getRetryCount() + 1;
                task.setRetryCount(count);
                task.setLastRetryAt(Instant.now());
                task.setErrorMessage(e.getMessage());

                if (count >= task.getMaxRetry()) {
                    task.setStatus(RetryTaskStatus.DEAD_LETTER);
                    log.error("Retry task publicId: {} failed and reached max retries. Moved to DEAD_LETTER.", task.getPublicId());
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
}
