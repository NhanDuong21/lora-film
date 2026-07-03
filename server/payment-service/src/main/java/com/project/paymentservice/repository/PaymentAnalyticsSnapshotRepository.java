package com.project.paymentservice.repository;

import com.project.paymentservice.entity.PaymentAnalyticsSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentAnalyticsSnapshotRepository extends JpaRepository<PaymentAnalyticsSnapshot, Long> {

    Optional<PaymentAnalyticsSnapshot> findByPaymentId(Long paymentId);
}
