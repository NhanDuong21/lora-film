package com.project.paymentservice.repository;

import com.project.paymentservice.entity.PaymentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentLogRepository extends JpaRepository<PaymentLog, Long> {

    List<PaymentLog> findByPaymentIdOrderByCreatedAtAscIdAsc(Long paymentId);
}
