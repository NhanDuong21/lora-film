package com.project.paymentservice.repository;

import com.project.paymentservice.entity.PaymentWebhookEvent;
import com.project.paymentservice.enumtype.WebhookProcessingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, Long> {

    Optional<PaymentWebhookEvent> findByProviderAndDeduplicationKey(String provider, String deduplicationKey);

    Page<PaymentWebhookEvent> findByPaymentId(Long paymentId, Pageable pageable);

    Page<PaymentWebhookEvent> findByProcessingStatus(WebhookProcessingStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM PaymentWebhookEvent w WHERE w.id = :id")
    Optional<PaymentWebhookEvent> findAndLockById(@Param("id") Long id);
}
