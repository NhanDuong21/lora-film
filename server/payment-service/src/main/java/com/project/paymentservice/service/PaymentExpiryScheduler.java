package com.project.paymentservice.service;

import com.project.paymentservice.entity.Payment;
import com.project.paymentservice.enumtype.PaymentStatus;
import com.project.paymentservice.repository.PaymentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class PaymentExpiryScheduler {
    private final PaymentRepository paymentRepository;
    private final PaymentTransactionService transactionService;

    public PaymentExpiryScheduler(
            PaymentRepository paymentRepository,
            PaymentTransactionService transactionService) {
        this.paymentRepository = paymentRepository;
        this.transactionService = transactionService;
    }

    @Scheduled(
            fixedDelayString = "${payment.runtime.expiry-fixed-delay-millis:5000}",
            initialDelayString = "${payment.runtime.expiry-initial-delay-millis:5000}")
    public void expirePastDeadlineAttempts() {
        Instant now = Instant.now();
        List<Payment> due = paymentRepository.findByStatusAndBookingExpiresAtBefore(
                PaymentStatus.PENDING, now, PageRequest.of(0, 50)).getContent();
        due.forEach(payment -> transactionService.expireAttempt(payment.getId(), now));
    }
}
