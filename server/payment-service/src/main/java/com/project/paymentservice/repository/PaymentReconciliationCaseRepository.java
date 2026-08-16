package com.project.paymentservice.repository;

import com.project.paymentservice.entity.PaymentReconciliationCase;
import com.project.paymentservice.enumtype.ReconciliationCaseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.List;

public interface PaymentReconciliationCaseRepository
        extends JpaRepository<PaymentReconciliationCase, Long> {
    Optional<PaymentReconciliationCase> findByPublicId(String publicId);
    Optional<PaymentReconciliationCase> findByPaymentIdAndReasonCodeAndSourceReference(
            Long paymentId, String reasonCode, String sourceReference);
    Page<PaymentReconciliationCase> findByStatus(ReconciliationCaseStatus status, Pageable pageable);
    java.util.List<PaymentReconciliationCase> findByPaymentIdOrderByCreatedAtDesc(Long paymentId);
    long countByStatusIn(List<ReconciliationCaseStatus> statuses);

    @Query("select count(c) from PaymentReconciliationCase c "
            + "where c.status in :statuses and c.openedAt >= :periodStart "
            + "and c.openedAt < :periodEndExclusive")
    long countOpenInPeriod(
            @Param("statuses") List<ReconciliationCaseStatus> statuses,
            @Param("periodStart") Instant periodStart,
            @Param("periodEndExclusive") Instant periodEndExclusive);

    @Query("select count(c) from PaymentReconciliationCase c, PaymentAnalyticsSnapshot s "
            + "where s.paymentId = c.paymentId and s.cinemaPublicId = :cinemaPublicId "
            + "and c.status in :statuses and c.openedAt >= :periodStart "
            + "and c.openedAt < :periodEndExclusive")
    long countOpenInPeriodForCinema(
            @Param("statuses") List<ReconciliationCaseStatus> statuses,
            @Param("cinemaPublicId") String cinemaPublicId,
            @Param("periodStart") Instant periodStart,
            @Param("periodEndExclusive") Instant periodEndExclusive);
}
