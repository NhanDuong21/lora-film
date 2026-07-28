package com.project.paymentservice.controller;

import com.project.paymentservice.common.ApiResponse;
import com.project.paymentservice.dto.request.CashCancelRequest;
import com.project.paymentservice.dto.request.CashCollectRequest;
import com.project.paymentservice.dto.request.CreateCashPaymentRequest;
import com.project.paymentservice.dto.response.CashCancelResponse;
import com.project.paymentservice.dto.response.CashCollectResponse;
import com.project.paymentservice.dto.response.CreatePaymentResponse;
import com.project.paymentservice.dto.response.EmployeeBookingPaymentResponse;
import com.project.paymentservice.dto.response.PaymentDetailResponse;
import com.project.paymentservice.exception.BusinessException;
import com.project.paymentservice.security.CurrentUserProvider;
import com.project.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employee/payments")
public class EmployeePaymentController {
    private final PaymentService paymentService;
    private final CurrentUserProvider currentUserProvider;

    public EmployeePaymentController(
            PaymentService paymentService, CurrentUserProvider currentUserProvider) {
        this.paymentService = paymentService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/booking")
    public ResponseEntity<ApiResponse<EmployeeBookingPaymentResponse>> lookupBooking(
            @RequestParam String reference) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.lookupBookingForCash(reference)));
    }

    @PostMapping("/cash")
    public ResponseEntity<ApiResponse<CreatePaymentResponse>> createCash(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateCashPaymentRequest request) {
        requireKey(idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Đã tạo giao dịch tiền mặt",
                paymentService.createCashPayment(
                        currentUserProvider.getCurrentUserId(), idempotencyKey, request)));
    }

    @GetMapping("/{paymentPublicId:[a-fA-F0-9-]{36}}")
    public ResponseEntity<ApiResponse<PaymentDetailResponse>> getPayment(
            @PathVariable String paymentPublicId) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.getPaymentForEmployee(paymentPublicId)));
    }

    @PostMapping("/{paymentPublicId:[a-fA-F0-9-]{36}}/cash/collect")
    public ResponseEntity<ApiResponse<CashCollectResponse>> collectCash(
            @PathVariable String paymentPublicId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CashCollectRequest request) {
        requireKey(idempotencyKey);
        CashCollectResponse response = paymentService.collectCashPayment(
                currentUserProvider.getCurrentUserId(), idempotencyKey, paymentPublicId, request);
        return ResponseEntity.status("PENDING".equals(response.getBookingDeliveryStatus())
                ? HttpStatus.ACCEPTED : HttpStatus.OK)
                .body(ApiResponse.success("Đã ghi nhận thu tiền mặt", response));
    }

    @PostMapping("/{paymentPublicId:[a-fA-F0-9-]{36}}/cash/cancel")
    public ResponseEntity<ApiResponse<CashCancelResponse>> cancelCash(
            @PathVariable String paymentPublicId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody(required = false) CashCancelRequest request) {
        requireKey(idempotencyKey);
        CashCancelRequest actual = request == null ? new CashCancelRequest() : request;
        return ResponseEntity.ok(ApiResponse.success("Đã hủy giao dịch tiền mặt",
                paymentService.cancelCashPayment(
                        currentUserProvider.getCurrentUserId(),
                        idempotencyKey,
                        paymentPublicId,
                        actual)));
    }

    @PostMapping("/{paymentId:\\d+}/cash/collect")
    public ResponseEntity<ApiResponse<CashCollectResponse>> collectCompat(
            @PathVariable Long paymentId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CashCollectRequest request) {
        requireKey(idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(paymentService.collectCashPayment(
                currentUserProvider.getCurrentUserId(), idempotencyKey, paymentId, request)));
    }

    @PostMapping("/{paymentId:\\d+}/cash/cancel")
    public ResponseEntity<ApiResponse<CashCancelResponse>> cancelCompat(
            @PathVariable Long paymentId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody(required = false) CashCancelRequest request) {
        requireKey(idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(paymentService.cancelCashPayment(
                currentUserProvider.getCurrentUserId(),
                idempotencyKey,
                paymentId,
                request == null ? new CashCancelRequest() : request)));
    }

    private void requireKey(String value) {
        if (value == null || value.isBlank() || value.length() > 100) {
            throw new BusinessException("IDEMPOTENCY_KEY_REQUIRED",
                    "Idempotency-Key là bắt buộc", HttpStatus.BAD_REQUEST);
        }
    }
}
