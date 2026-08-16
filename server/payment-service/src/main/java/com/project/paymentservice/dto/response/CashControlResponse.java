package com.project.paymentservice.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record CashControlResponse(
        String publicId,
        Long employeeAccountId,
        String employeeCode,
        String employeeName,
        String employeeAvatarUrl,
        String employeePositionCode,
        String employeePositionName,
        String cinemaPublicId,
        String status,
        String verificationStatus,
        BigDecimal openingFloat,
        BigDecimal cashSales,
        Long cashTransactionCount,
        BigDecimal cashRefunds,
        Long cashRefundCount,
        BigDecimal expectedCash,
        BigDecimal countedCash,
        BigDecimal varianceAmount,
        String closingNote,
        Long verifiedByAccountId,
        String verifiedByName,
        String verifiedByAvatarUrl,
        Instant verifiedAt,
        String verificationNote,
        Instant openedAt,
        Instant closedAt,
        Integer version,
        boolean canVerify,
        String verifyBlockedReason) {
}
