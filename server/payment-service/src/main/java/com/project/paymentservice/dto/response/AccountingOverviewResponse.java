package com.project.paymentservice.dto.response;

import java.math.BigDecimal;

public record AccountingOverviewResponse(
        String cinemaPublicId,
        long settlementBatchesNeedReview,
        long reconciliationCasesOpen,
        long cashSessionsNeedVerification,
        BigDecimal cashVarianceNeedReview,
        long accountingPeriodsOpen) {
}
