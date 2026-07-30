package com.project.paymentservice.repository;

import com.project.paymentservice.entity.PaymentAnalyticsSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentAnalyticsSnapshotRepository extends JpaRepository<PaymentAnalyticsSnapshot, Long> {

    Optional<PaymentAnalyticsSnapshot> findByPaymentId(Long paymentId);

    @Query("""
            select s.payment.id
            from PaymentAnalyticsSnapshot s
            where s.showtimePublicId = :showtimePublicId
              and s.payment.status = com.project.paymentservice.enumtype.PaymentStatus.SUCCESS
            order by s.payment.id
            """)
    List<Long> findSuccessfulPaymentIdsByShowtimePublicId(
            @Param("showtimePublicId") String showtimePublicId);
}
