package com.project.analyticsservice.repository;

import com.project.analyticsservice.entity.FactPaymentRefund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FactPaymentRefundRepository extends JpaRepository<FactPaymentRefund, Long> {
    List<FactPaymentRefund> findAllByRefundDate(LocalDate refundDate);
    List<FactPaymentRefund> findAllByRefundDateBetween(LocalDate startDate, LocalDate endDate);
    List<FactPaymentRefund> findAllByRefundDateLessThanEqual(LocalDate endDate);
}
