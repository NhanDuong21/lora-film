package com.lorafilm.booking.payment.scheduler;

import com.lorafilm.booking.infrastructure.entity.BookingDeadLetterEvent;
import com.lorafilm.booking.infrastructure.entity.BookingRetryTask;
import com.lorafilm.booking.infrastructure.enums.RetryTaskStatus;
import com.lorafilm.booking.infrastructure.enums.RetryTaskType;
import com.lorafilm.booking.infrastructure.repository.BookingDeadLetterEventRepository;
import com.lorafilm.booking.infrastructure.repository.BookingRetryTaskRepository;
import com.lorafilm.booking.payment.event.PaymentEventConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetryTaskSchedulerTest {

    @Mock
    private BookingRetryTaskRepository retryTaskRepository;

    @Mock
    private PaymentEventConsumer paymentEventConsumer;

    @Mock
    private BookingDeadLetterEventRepository deadLetterEventRepository;

    @Test
    void processRetryTasksExecutesInboxTask() {
        BookingRetryTask task = task(RetryTaskType.INBOX_PROCESS);
        when(retryTaskRepository.findByStatusAndNextRetryAtBefore(
                eq(RetryTaskStatus.PENDING), any(Instant.class), any()))
                .thenReturn(List.of(task));

        scheduler().processRetryTasks();

        verify(paymentEventConsumer).consume("{\"eventId\":\"evt-1\"}");
        assertThat(task.getStatus()).isEqualTo(RetryTaskStatus.SUCCESS);
        assertThat(task.getErrorCode()).isNull();
        verify(deadLetterEventRepository, never()).save(any());
    }

    @Test
    void processRetryTasksDeadLettersTaskWithoutRegisteredHandler() {
        BookingRetryTask task = task(RetryTaskType.REFUND);
        task.setMaxRetry(5);
        when(retryTaskRepository.findByStatusAndNextRetryAtBefore(
                eq(RetryTaskStatus.PENDING), any(Instant.class), any()))
                .thenReturn(List.of(task));

        scheduler().processRetryTasks();

        assertThat(task.getStatus()).isEqualTo(RetryTaskStatus.DEAD_LETTER);
        assertThat(task.getRetryCount()).isEqualTo(5);
        assertThat(task.getErrorCode()).isEqualTo("NO_RETRY_HANDLER");
        verify(paymentEventConsumer, never()).consume(any());

        ArgumentCaptor<BookingDeadLetterEvent> captor =
                ArgumentCaptor.forClass(BookingDeadLetterEvent.class);
        verify(deadLetterEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("REFUND");
        assertThat(captor.getValue().getErrorCode()).isEqualTo("NO_RETRY_HANDLER");
    }

    private RetryTaskScheduler scheduler() {
        return new RetryTaskScheduler(
                retryTaskRepository,
                paymentEventConsumer,
                deadLetterEventRepository);
    }

    private BookingRetryTask task(RetryTaskType type) {
        BookingRetryTask task = new BookingRetryTask();
        task.setPublicId("retry-1");
        task.setTaskType(type);
        task.setReferenceType("PAYMENT_EVENT");
        task.setReferenceId(1L);
        task.setPayload("{\"eventId\":\"evt-1\"}");
        task.setRetryCount(0);
        task.setMaxRetry(3);
        task.setStatus(RetryTaskStatus.PENDING);
        task.setNextRetryAt(Instant.now().minusSeconds(1));
        return task;
    }
}
