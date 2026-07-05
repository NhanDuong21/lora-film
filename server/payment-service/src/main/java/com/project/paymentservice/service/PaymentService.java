package com.project.paymentservice.service;

import com.project.paymentservice.dto.request.CreatePaymentRequest;
import com.project.paymentservice.dto.request.MockCallbackRequest;
import com.project.paymentservice.dto.response.CancelPaymentResponse;
import com.project.paymentservice.dto.response.CreatePaymentResponse;
import com.project.paymentservice.dto.response.PaymentDetailResponse;
import com.project.paymentservice.dto.response.PaymentStatusResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentService {

    CreatePaymentResponse createPayment(Long accountId, String idempotencyKey, CreatePaymentRequest request);

    PaymentDetailResponse getPayment(Long accountId, Long paymentId);

    PaymentStatusResponse getPaymentStatus(Long accountId, Long paymentId);

    Page<PaymentDetailResponse> getPaymentsByBooking(Long accountId, Long bookingId, Pageable pageable);

    CancelPaymentResponse cancelPayment(Long accountId, String idempotencyKey, Long paymentId);

    void processMockCallback(MockCallbackRequest request);
}
