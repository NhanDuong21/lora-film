package com.project.paymentservice.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.project.paymentservice.common.MoneyJsonSerializer;

import java.math.BigDecimal;
import java.time.Instant;

public record CounterCashSessionResponse(
        String publicId,
        String status,
        Long employeeAccountId,
        String cinemaPublicId,
        @JsonSerialize(using = MoneyJsonSerializer.class) BigDecimal openingFloat,
        @JsonSerialize(using = MoneyJsonSerializer.class) BigDecimal cashSales,
        long cashTransactionCount,
        @JsonSerialize(using = MoneyJsonSerializer.class) BigDecimal cashRefunds,
        long cashRefundCount,
        @JsonSerialize(using = MoneyJsonSerializer.class) BigDecimal expectedCash,
        @JsonSerialize(using = MoneyJsonSerializer.class) BigDecimal countedCash,
        @JsonSerialize(using = MoneyJsonSerializer.class) BigDecimal varianceAmount,
        String openingNote,
        String closingNote,
        Instant openedAt,
        Instant closedAt) {
}
