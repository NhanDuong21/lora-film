package com.project.paymentservice.repository;

import com.project.paymentservice.entity.PaymentRefund;
import com.project.paymentservice.enumtype.RefundComponent;
import com.project.paymentservice.enumtype.RefundStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, Long> {
    Optional<PaymentRefund> findByPublicId(String publicId);
    Optional<PaymentRefund> findByPaymentIdAndRequestKey(Long paymentId, String requestKey);
    List<PaymentRefund> findByPaymentIdOrderByCreatedAtDesc(Long paymentId);
    Page<PaymentRefund> findByStatus(RefundStatus status, Pageable pageable);

    Page<PaymentRefund> findByPaymentIdIn(Collection<Long> paymentIds, Pageable pageable);
    Page<PaymentRefund> findByPaymentIdInAndStatus(
            Collection<Long> paymentIds, RefundStatus status, Pageable pageable);

    @Query("""
            select coalesce(sum(r.requestedAmount), 0)
            from PaymentRefund r
            where r.providerCode = com.project.paymentservice.enumtype.ProviderCode.CASH
              and r.status = com.project.paymentservice.enumtype.RefundStatus.SUCCESS
              and r.completedByAccountId = :employeeAccountId
              and r.succeededAt >= :from
              and r.succeededAt <= :to
            """)
    BigDecimal sumSuccessfulCashRefunds(
            @Param("employeeAccountId") Long employeeAccountId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("""
            select count(r)
            from PaymentRefund r
            where r.providerCode = com.project.paymentservice.enumtype.ProviderCode.CASH
              and r.status = com.project.paymentservice.enumtype.RefundStatus.SUCCESS
              and r.completedByAccountId = :employeeAccountId
              and r.succeededAt >= :from
              and r.succeededAt <= :to
            """)
    long countSuccessfulCashRefunds(
            @Param("employeeAccountId") Long employeeAccountId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PaymentRefund r where r.id = :id")
    Optional<PaymentRefund> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select coalesce(sum(r.requestedAmount), 0)
            from PaymentRefund r
            where r.payment.id = :paymentId and r.status in :statuses
            """)
    BigDecimal sumReservedAmount(
            @Param("paymentId") Long paymentId,
            @Param("statuses") Collection<RefundStatus> statuses);

    @Query("""
            select coalesce(sum(r.requestedAmount), 0)
            from PaymentRefund r
            where r.payment.id = :paymentId
              and r.refundComponent = :component
              and r.status in :statuses
            """)
    BigDecimal sumReservedAmountByComponent(
            @Param("paymentId") Long paymentId,
            @Param("component") RefundComponent component,
            @Param("statuses") Collection<RefundStatus> statuses);

    @Query("""
            select r from PaymentRefund r
            where r.status in :statuses
              and (r.nextAttemptAt is null or r.nextAttemptAt <= :now)
              and (r.lockedUntil is null or r.lockedUntil <= :now)
            order by r.requestedAt asc
            """)
    List<PaymentRefund> findReady(
            @Param("statuses") Collection<RefundStatus> statuses,
            @Param("now") Instant now,
            Pageable pageable);
}
