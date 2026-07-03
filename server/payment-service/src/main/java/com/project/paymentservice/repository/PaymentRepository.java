package com.project.paymentservice.repository;

import com.project.paymentservice.entity.Payment;
import com.project.paymentservice.enumtype.PaymentMethod;
import com.project.paymentservice.enumtype.PaymentStatus;
import com.project.paymentservice.enumtype.ReconciliationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentTransactionCode(String paymentTransactionCode);

    Page<Payment> findByBookingId(Long bookingId, Pageable pageable);

    Page<Payment> findByAccountId(Long accountId, Pageable pageable);

    Optional<Payment> findByBookingIdAndAttemptNumber(Long bookingId, Integer attemptNumber);

    Optional<Payment> findByPaymentMethodAndExternalTransactionId(PaymentMethod paymentMethod,
            String externalTransactionId);

    Page<Payment> findByReconciliationStatus(ReconciliationStatus reconciliationStatus, Pageable pageable);

    Page<Payment> findByStatusAndExpiresAtBefore(PaymentStatus status, LocalDateTime expiresAt, Pageable pageable);

    Page<Payment> findByStatusAndSettlementHoldUntilBefore(PaymentStatus status, LocalDateTime now, Pageable pageable);
}
