package com.project.paymentservice.dto.response;

import java.math.BigDecimal;

public record SettlementEntryResponse(
        Long id,
        Long paymentId,
        String paymentTransactionCode,
        BigDecimal loraFilmAmount,
        String loraFilmPaymentStatus,
        String providerTransactionId,
        String bankCreditReference,
        BigDecimal providerGrossAmount,
        BigDecimal providerFeeAmount,
        BigDecimal providerNetAmount,
        BigDecimal bankCreditAmount,
        String status,
        String mismatchReason) {
}
