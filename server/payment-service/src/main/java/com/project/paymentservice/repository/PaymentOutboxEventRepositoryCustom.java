package com.project.paymentservice.repository;

import com.project.paymentservice.entity.PaymentOutboxEvent;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentOutboxEventRepositoryCustom {

    List<PaymentOutboxEvent> findAndClaimPendingEvents(LocalDateTime now, int batchSize);
}
