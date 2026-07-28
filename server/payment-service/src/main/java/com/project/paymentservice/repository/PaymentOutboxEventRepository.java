package com.project.paymentservice.repository;

import com.project.paymentservice.entity.PaymentOutboxEvent;
import com.project.paymentservice.enumtype.OutboxStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentOutboxEventRepository extends JpaRepository<PaymentOutboxEvent, Long>, PaymentOutboxEventRepositoryCustom {

    Optional<PaymentOutboxEvent> findByEventId(String eventId);

    Page<PaymentOutboxEvent> findByStatus(OutboxStatus status, Pageable pageable);

    List<PaymentOutboxEvent> findByAggregateIdAndStatus(String aggregateId, OutboxStatus status);
    List<PaymentOutboxEvent> findByAggregateIdOrderByCreatedAtDesc(String aggregateId);

    @Query("SELECT o FROM PaymentOutboxEvent o WHERE o.status = :status AND (o.nextRetryAt IS NULL OR o.nextRetryAt <= :now)")
    Page<PaymentOutboxEvent> findRetryableRecords(@Param("status") OutboxStatus status, @Param("now") Instant now,
            Pageable pageable);

    Page<PaymentOutboxEvent> findByAggregateTypeAndAggregateId(String aggregateType, String aggregateId,
            Pageable pageable);

    Page<PaymentOutboxEvent> findByStatusIn(List<OutboxStatus> statuses, Pageable pageable);
}
