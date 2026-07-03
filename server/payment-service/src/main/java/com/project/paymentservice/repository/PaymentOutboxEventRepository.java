package com.project.paymentservice.repository;

import com.project.paymentservice.entity.PaymentOutboxEvent;
import com.project.paymentservice.enumtype.OutboxStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentOutboxEventRepository extends JpaRepository<PaymentOutboxEvent, Long> {

    Optional<PaymentOutboxEvent> findByEventId(String eventId);

    Page<PaymentOutboxEvent> findByStatus(OutboxStatus status, Pageable pageable);

    @Query("SELECT o FROM PaymentOutboxEvent o WHERE o.status = :status AND (o.nextRetryAt IS NULL OR o.nextRetryAt <= :now)")
    Page<PaymentOutboxEvent> findRetryableRecords(@Param("status") OutboxStatus status, @Param("now") LocalDateTime now, Pageable pageable);

    Page<PaymentOutboxEvent> findByAggregateTypeAndAggregateId(String aggregateType, String aggregateId, Pageable pageable);

    Page<PaymentOutboxEvent> findByStatusIn(List<OutboxStatus> statuses, Pageable pageable);

    @Query(value = "SELECT * FROM payment_outbox_events WHERE status = 'PENDING' AND (next_retry_at IS NULL OR next_retry_at <= :now) ORDER BY created_at LIMIT :batchSize FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<PaymentOutboxEvent> findAndClaimPendingEvents(@Param("now") LocalDateTime now, @Param("batchSize") int batchSize);
}
