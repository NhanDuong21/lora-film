package com.project.paymentservice.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AccountingPeriodResponse(
        String publicId,
        String periodCode,
        String cinemaPublicId,
        LocalDate periodStart,
        LocalDate periodEnd,
        String status,
        Long createdByAccountId,
        Long reconciledByAccountId,
        Instant reconciledAt,
        Long lockedByAccountId,
        Instant lockedAt,
        String note,
        Integer version,
        Instant createdAt,
        List<String> blockers,
        boolean canReconcile,
        boolean canLock) {
}
