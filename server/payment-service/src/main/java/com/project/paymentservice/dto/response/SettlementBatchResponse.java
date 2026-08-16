package com.project.paymentservice.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record SettlementBatchResponse(
        String publicId,
        String providerCode,
        String batchCode,
        String cinemaPublicId,
        LocalDate periodStart,
        LocalDate periodEnd,
        String sourceFileName,
        String status,
        Integer entryCount,
        Integer matchedCount,
        Integer mismatchCount,
        BigDecimal grossAmount,
        BigDecimal feeAmount,
        BigDecimal providerNetAmount,
        BigDecimal bankCreditAmount,
        Long createdByAccountId,
        Long lockedByAccountId,
        Instant lockedAt,
        String note,
        Integer version,
        Instant createdAt,
        List<SettlementEntryResponse> entries,
        boolean canLock,
        String lockBlockedReason) {
}
