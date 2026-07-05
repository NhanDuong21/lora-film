package com.project.paymentservice.repository;

import com.project.paymentservice.entity.CashPaymentDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CashPaymentDetailRepository extends JpaRepository<CashPaymentDetail, Long> {
}
