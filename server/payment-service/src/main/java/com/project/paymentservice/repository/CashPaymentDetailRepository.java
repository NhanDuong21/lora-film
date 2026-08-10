package com.project.paymentservice.repository;

import com.project.paymentservice.entity.CashPaymentDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;

@Repository
public interface CashPaymentDetailRepository extends JpaRepository<CashPaymentDetail, Long> {
    @Query("""
            select coalesce(sum(c.payment.amount), 0)
            from CashPaymentDetail c
            where c.collectedByAccountId = :employeeAccountId
              and c.collectedAt >= :from
              and c.collectedAt <= :to
              and c.payment.status = com.project.paymentservice.enumtype.PaymentStatus.SUCCESS
            """)
    BigDecimal sumSuccessfulCashCollected(
            @Param("employeeAccountId") Long employeeAccountId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("""
            select count(c)
            from CashPaymentDetail c
            where c.collectedByAccountId = :employeeAccountId
              and c.collectedAt >= :from
              and c.collectedAt <= :to
              and c.payment.status = com.project.paymentservice.enumtype.PaymentStatus.SUCCESS
            """)
    long countSuccessfulCashCollected(
            @Param("employeeAccountId") Long employeeAccountId,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
