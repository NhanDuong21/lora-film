package com.project.paymentservice.repository;

import com.project.paymentservice.entity.PaymentIdempotencyRecord;
import com.project.paymentservice.enumtype.IdempotencyProcessingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PaymentIdempotencyRecordRepository extends JpaRepository<PaymentIdempotencyRecord, Long> {

    Optional<PaymentIdempotencyRecord> findByAccountIdAndOperationAndIdempotencyKey(Long accountId, String operation,
            String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM PaymentIdempotencyRecord r WHERE r.accountId = :accountId AND r.operation = :operation AND r.idempotencyKey = :idempotencyKey")
    Optional<PaymentIdempotencyRecord> findAndLockByAccountIdAndOperationAndIdempotencyKey(
            @Param("accountId") Long accountId,
            @Param("operation") String operation,
            @Param("idempotencyKey") String idempotencyKey);

    Page<PaymentIdempotencyRecord> findByProcessingStatusAndLockedAtBefore(IdempotencyProcessingStatus status,
            LocalDateTime lockedAt, Pageable pageable);

    Page<PaymentIdempotencyRecord> findByExpiresAtBefore(LocalDateTime expiresAt, Pageable pageable);
}
