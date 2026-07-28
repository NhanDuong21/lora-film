package com.project.paymentservice.service;

import com.project.paymentservice.config.PaymentRuntimeProperties;
import com.project.paymentservice.entity.PaymentIdempotencyRecord;
import com.project.paymentservice.enumtype.IdempotencyProcessingStatus;
import com.project.paymentservice.exception.BusinessException;
import com.project.paymentservice.repository.PaymentIdempotencyRecordRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class PaymentIdempotencyService {
    private final PaymentIdempotencyRecordRepository repository;
    private final PaymentRuntimeProperties properties;

    public PaymentIdempotencyService(
            PaymentIdempotencyRecordRepository repository,
            PaymentRuntimeProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Reservation reserve(Long accountId, String operation, String key,
            String requestHash, String ownerToken) {
        Instant now = Instant.now();
        repository.insertIfAbsent(
                accountId,
                operation,
                key,
                requestHash,
                ownerToken,
                now,
                now.plusSeconds(properties.getIdempotencyLeaseSeconds()),
                now.plusSeconds(properties.getIdempotencyTtlSeconds()));
        PaymentIdempotencyRecord record = repository
                .findAndLockByAccountIdAndOperationAndIdempotencyKey(accountId, operation, key)
                .orElseThrow(() -> new IllegalStateException("Cannot reserve idempotency record"));
        if (!requestHash.equals(record.getRequestHash())) {
            throw new BusinessException(
                    "IDEMPOTENCY_KEY_REUSED",
                    "Khóa chống trùng đã được dùng cho một yêu cầu khác",
                    HttpStatus.CONFLICT);
        }
        if (record.getProcessingStatus() == IdempotencyProcessingStatus.COMPLETED) {
            return new Reservation(record, true, false);
        }
        if (record.getProcessingStatus() == IdempotencyProcessingStatus.FAILED) {
            throw new BusinessException(
                    record.getErrorCode() == null ? "PAYMENT_OPERATION_FAILED" : record.getErrorCode(),
                    record.getLastErrorSanitized() == null
                            ? "Yêu cầu thanh toán trước đó đã thất bại"
                            : record.getLastErrorSanitized(),
                    HttpStatus.CONFLICT);
        }
        boolean recovery = record.getPaymentId() != null;
        boolean owned = ownerToken.equals(record.getLockedBy());
        boolean leaseExpired = record.getLockedUntil() == null || !record.getLockedUntil().isAfter(now);
        if (!owned && !leaseExpired) {
            throw new BusinessException(
                    "PAYMENT_REQUEST_IN_PROGRESS",
                    "Yêu cầu thanh toán đang được xử lý",
                    HttpStatus.CONFLICT);
        }
        record.setLockedBy(ownerToken);
        record.setLockedAt(now);
        record.setLockedUntil(now.plusSeconds(properties.getIdempotencyLeaseSeconds()));
        repository.save(record);
        return new Reservation(record, false, recovery);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void attachPayment(Long recordId, String ownerToken, Long paymentId) {
        PaymentIdempotencyRecord record = repository.findById(recordId)
                .orElseThrow(() -> new IllegalStateException("Idempotency record not found"));
        requireOwner(record, ownerToken);
        record.setPaymentId(paymentId);
        repository.save(record);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(Long recordId, String ownerToken, Long paymentId,
            int responseStatus, String responseBody) {
        PaymentIdempotencyRecord record = repository.findById(recordId)
                .orElseThrow(() -> new IllegalStateException("Idempotency record not found"));
        requireOwner(record, ownerToken);
        record.setPaymentId(paymentId);
        record.setResponseStatus(responseStatus);
        record.setResponseBodySanitized(responseBody);
        record.setProcessingStatus(IdempotencyProcessingStatus.COMPLETED);
        clearLease(record);
        repository.save(record);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(Long recordId, String ownerToken, String errorCode, String safeMessage) {
        PaymentIdempotencyRecord record = repository.findById(recordId)
                .orElseThrow(() -> new IllegalStateException("Idempotency record not found"));
        requireOwner(record, ownerToken);
        record.setErrorCode(errorCode);
        record.setLastErrorSanitized(safeMessage);
        record.setProcessingStatus(IdempotencyProcessingStatus.FAILED);
        clearLease(record);
        repository.save(record);
    }

    private void requireOwner(PaymentIdempotencyRecord record, String ownerToken) {
        if (!ownerToken.equals(record.getLockedBy())) {
            throw new BusinessException(
                    "IDEMPOTENCY_OWNER_MISMATCH",
                    "Yêu cầu thanh toán không còn quyền hoàn tất khóa chống trùng",
                    HttpStatus.CONFLICT);
        }
    }

    private void clearLease(PaymentIdempotencyRecord record) {
        record.setLockedBy(null);
        record.setLockedAt(null);
        record.setLockedUntil(null);
    }

    public record Reservation(
            PaymentIdempotencyRecord record,
            boolean replay,
            boolean recovery) {
    }
}
