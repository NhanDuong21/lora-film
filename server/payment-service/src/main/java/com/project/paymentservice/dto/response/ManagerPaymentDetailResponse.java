package com.project.paymentservice.dto.response;

import java.util.List;
import java.util.Map;

public record ManagerPaymentDetailResponse(
        PaymentDetailResponse payment,
        Map<String, Object> bookingSnapshot,
        List<RefundResponse> refundRequests) {
}
