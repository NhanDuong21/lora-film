package com.project.paymentservice;

import com.project.paymentservice.entity.Payment;
import com.project.paymentservice.enumtype.PaymentStatus;
import com.project.paymentservice.repository.PaymentRepository;
import com.project.paymentservice.service.PaymentExpiryScheduler;
import com.project.paymentservice.service.PaymentTransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentExpirySchedulerTest {

    @Test
    void expiresOnlyPendingAttemptsAndLeavesProcessingToProviderRecovery() {
        PaymentRepository repository = mock(PaymentRepository.class);
        PaymentTransactionService transactionService = mock(PaymentTransactionService.class);
        Payment pending = new Payment();
        pending.setId(41L);

        when(repository.findByStatusAndBookingExpiresAtBefore(
                eq(PaymentStatus.PENDING), any(Instant.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(pending)));

        PaymentExpiryScheduler scheduler =
                new PaymentExpiryScheduler(repository, transactionService);
        scheduler.expirePastDeadlineAttempts();

        verify(transactionService).expireAttempt(eq(41L), any(Instant.class));
        verify(repository, never()).findByStatusAndBookingExpiresAtBefore(
                eq(PaymentStatus.PROCESSING), any(Instant.class), any(Pageable.class));
    }
}
