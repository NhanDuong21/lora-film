package com.project.paymentservice.service;

import com.project.paymentservice.dto.request.CashCancelRequest;
import com.project.paymentservice.dto.request.CashCollectRequest;
import com.project.paymentservice.dto.request.CreateCashPaymentRequest;
import com.project.paymentservice.dto.request.CreatePaymentRequest;
import com.project.paymentservice.dto.request.MockCallbackRequest;
import com.project.paymentservice.dto.response.CancelPaymentResponse;
import com.project.paymentservice.dto.response.CashCancelResponse;
import com.project.paymentservice.dto.response.CashCollectResponse;
import com.project.paymentservice.dto.response.CreatePaymentResponse;
import com.project.paymentservice.dto.response.EmployeeBookingPaymentResponse;
import com.project.paymentservice.dto.response.PaymentDetailResponse;
import com.project.paymentservice.dto.response.PaymentStatusResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentService {
    CreatePaymentResponse createPayment(
            Long accountId, String idempotencyKey, CreatePaymentRequest request);
    CreatePaymentResponse createPayment(
            Long accountId, String idempotencyKey, CreatePaymentRequest request, String clientIp);
    PaymentDetailResponse getPayment(Long accountId, String paymentPublicId);
    PaymentDetailResponse getPayment(Long accountId, Long paymentId);
    PaymentStatusResponse getPaymentStatus(Long accountId, String paymentPublicId);
    PaymentStatusResponse getPaymentStatus(Long accountId, Long paymentId);
    Page<PaymentDetailResponse> getPaymentsByBooking(
            Long accountId, String bookingPublicId, Pageable pageable);
    Page<PaymentDetailResponse> getPaymentsByBooking(
            Long accountId, Long bookingId, Pageable pageable);
    CancelPaymentResponse cancelPayment(
            Long accountId, String idempotencyKey, String paymentPublicId);
    CancelPaymentResponse cancelPayment(
            Long accountId, String idempotencyKey, Long paymentId);
    EmployeeBookingPaymentResponse lookupBookingForCash(String reference);
    PaymentDetailResponse getPaymentForEmployee(String paymentPublicId);
    CreatePaymentResponse createCashPayment(
            Long employeeId, String idempotencyKey, CreateCashPaymentRequest request);
    CashCollectResponse collectCashPayment(
            Long employeeId, String idempotencyKey, String paymentPublicId, CashCollectRequest request);
    CashCollectResponse collectCashPayment(
            Long employeeId, String idempotencyKey, Long paymentId, CashCollectRequest request);
    CashCancelResponse cancelCashPayment(
            Long employeeId, String idempotencyKey, String paymentPublicId, CashCancelRequest request);
    CashCancelResponse cancelCashPayment(
            Long employeeId, String idempotencyKey, Long paymentId, CashCancelRequest request);
    void processMockCallback(Long accountId, String paymentPublicId, String simulatedStatus);
    void processMockCallback(MockCallbackRequest request);
}
