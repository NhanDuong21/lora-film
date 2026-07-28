package com.project.paymentservice.mapper;

import com.project.paymentservice.dto.response.CreatePaymentResponse;
import com.project.paymentservice.dto.response.PaymentDetailResponse;
import com.project.paymentservice.entity.Payment;

public final class PaymentMapper {

    private PaymentMapper() {
    }

    public static CreatePaymentResponse toCreateResponse(Payment payment, String paymentUrl) {
        CreatePaymentResponse response = new CreatePaymentResponse();
        response.setPaymentId(payment.getId());
        response.setPaymentPublicId(payment.getPublicId());
        response.setBookingPublicId(payment.getBookingPublicId());
        response.setPaymentTransactionCode(payment.getPaymentTransactionCode());
        response.setPaymentMethod(payment.getPaymentMethod().name());
        response.setProvider(payment.getProviderCode().name());
        response.setPaymentUrl(paymentUrl);
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setStatus(payment.getStatus().name());
        response.setExpiresAt(payment.getExpiresAt());
        response.setCreatedAt(payment.getCreatedAt());
        return response;
    }

    public static PaymentDetailResponse toDetailResponse(Payment payment) {
        PaymentDetailResponse response = new PaymentDetailResponse();
        response.setPaymentId(payment.getId());
        response.setPaymentPublicId(payment.getPublicId());
        response.setPaymentTransactionCode(payment.getPaymentTransactionCode());
        response.setBookingId(payment.getBookingId());
        response.setBookingPublicId(payment.getBookingPublicId());
        response.setStatus(payment.getStatus().name());
        response.setPaymentMethod(payment.getPaymentMethod().name());
        response.setProvider(payment.getProviderCode().name());
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setAttemptNumber(payment.getAttemptNumber());
        response.setReconciliationStatus(payment.getReconciliationStatus().name());
        response.setExpiresAt(payment.getExpiresAt());
        response.setCreatedAt(payment.getCreatedAt());
        response.setUpdatedAt(payment.getUpdatedAt());
        response.setExternalTransactionId(payment.getExternalTransactionId());
        return response;
    }
}
