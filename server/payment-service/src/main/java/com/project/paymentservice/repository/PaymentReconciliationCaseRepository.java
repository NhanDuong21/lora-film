package com.project.paymentservice.repository;

import com.project.paymentservice.entity.PaymentReconciliationCase;
import com.project.paymentservice.enumtype.ReconciliationCaseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentReconciliationCaseRepository
        extends JpaRepository<PaymentReconciliationCase, Long> {
    Optional<PaymentReconciliationCase> findByPublicId(String publicId);
    Optional<PaymentReconciliationCase> findByPaymentIdAndReasonCodeAndSourceReference(
            Long paymentId, String reasonCode, String sourceReference);
    Page<PaymentReconciliationCase> findByStatus(ReconciliationCaseStatus status, Pageable pageable);
    java.util.List<PaymentReconciliationCase> findByPaymentIdOrderByCreatedAtDesc(Long paymentId);
}
