package com.project.paymentservice.repository;

import com.project.paymentservice.entity.CashPaymentDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CashPaymentDetailRepository extends JpaRepository<CashPaymentDetail, Long> {

    Optional<CashPaymentDetail> findByPaymentId(Long paymentId);

    boolean existsByPaymentId(Long paymentId);
}
