package com.project.paymentservice.dto.response;

import java.util.List;
import java.util.Map;

public record AdminPaymentDetailResponse(
        PaymentDetailResponse payment,
        Map<String, Object> analyticsSnapshot,
        Map<String, Object> cashDetail,
        List<Map<String, Object>> logs,
        List<Map<String, Object>> webhooks,
        List<Map<String, Object>> outboxEvents,
        List<Map<String, Object>> reconciliationCases,
        List<RefundResponse> refunds) {
}
