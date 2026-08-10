package com.project.paymentservice.repository;

import com.project.paymentservice.entity.PaymentIdempotencyRecord;
import com.project.paymentservice.enumtype.IdempotencyProcessingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PaymentIdempotencyRecordRepository extends JpaRepository<PaymentIdempotencyRecord, Long> {

    @Modifying
    @Query(value = """
            insert ignore into payment_idempotency_records
                (account_id, operation, idempotency_key, request_hash,
                 processing_status, locked_by, locked_at, locked_until, expires_at)
            values
                (:accountId, :operation, :idempotencyKey, :requestHash,
                 'PROCESSING', :ownerToken, :now, :lockedUntil, :expiresAt)
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("accountId") Long accountId,
            @Param("operation") String operation,
            @Param("idempotencyKey") String idempotencyKey,
            @Param("requestHash") String requestHash,
            @Param("ownerToken") String ownerToken,
            @Param("now") Instant now,
            @Param("lockedUntil") Instant lockedUntil,
            @Param("expiresAt") Instant expiresAt);

    Optional<PaymentIdempotencyRecord> findByAccountIdAndOperationAndIdempotencyKey(Long accountId, String operation,
            String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM PaymentIdempotencyRecord r WHERE r.accountId = :accountId AND r.operation = :operation AND r.idempotencyKey = :idempotencyKey")
    Optional<PaymentIdempotencyRecord> findAndLockByAccountIdAndOperationAndIdempotencyKey(
            @Param("accountId") Long accountId,
            @Param("operation") String operation,
            @Param("idempotencyKey") String idempotencyKey);

    Page<PaymentIdempotencyRecord> findByProcessingStatusAndLockedUntilBefore(IdempotencyProcessingStatus status,
            Instant lockedUntil, Pageable pageable);

    Page<PaymentIdempotencyRecord> findByExpiresAtBefore(Instant expiresAt, Pageable pageable);
}
