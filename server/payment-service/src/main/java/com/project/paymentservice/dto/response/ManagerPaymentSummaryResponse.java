package com.project.paymentservice.dto.response;

public record ManagerPaymentSummaryResponse(
        long totalTransactions,
        long successful,
        long processing,
        long failed,
        long needsFinanceReview) {
}
