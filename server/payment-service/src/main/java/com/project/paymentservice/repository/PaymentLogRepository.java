package com.project.paymentservice.repository;

import com.project.paymentservice.entity.PaymentLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentLogRepository extends JpaRepository<PaymentLog, Long> {

    Page<PaymentLog> findByPaymentIdOrderByCreatedAtAsc(Long paymentId, Pageable pageable);
}
