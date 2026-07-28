package com.project.paymentservice.repository;

import com.project.paymentservice.entity.PaymentOutboxEvent;

import java.time.Instant;
import java.util.List;

public interface PaymentOutboxEventRepositoryCustom {

    List<PaymentOutboxEvent> findAndClaimPendingEvents(
            Instant now, Instant lockedUntil, String ownerToken, int batchSize);
}
