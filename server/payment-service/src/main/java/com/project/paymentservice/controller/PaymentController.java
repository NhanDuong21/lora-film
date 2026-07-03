package com.project.paymentservice.controller;

import com.project.paymentservice.common.ApiResponse;
import com.project.paymentservice.dto.request.CreatePaymentRequest;
import com.project.paymentservice.dto.response.CancelPaymentResponse;
import com.project.paymentservice.dto.response.CreatePaymentResponse;
import com.project.paymentservice.dto.response.PaymentDetailResponse;
import com.project.paymentservice.dto.response.PaymentStatusResponse;
import com.project.paymentservice.exception.BusinessException;
import com.project.paymentservice.security.CurrentUserProvider;
import com.project.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final CurrentUserProvider currentUserProvider;

    public PaymentController(PaymentService paymentService, CurrentUserProvider currentUserProvider) {
        this.paymentService = paymentService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CreatePaymentResponse>> createPayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException("IDEMPOTENCY_KEY_REQUIRED",
                    "Idempotency-Key header is required", HttpStatus.BAD_REQUEST);
        }

        Long accountId = currentUserProvider.getCurrentUserId();
        CreatePaymentResponse response = paymentService.createPayment(accountId, idempotencyKey, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment created successfully", response));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentDetailResponse>> getPayment(
            @PathVariable Long paymentId) {

        Long accountId = currentUserProvider.getCurrentUserId();
        PaymentDetailResponse response = paymentService.getPayment(accountId, paymentId);

        return ResponseEntity.ok(ApiResponse.success("Payment retrieved successfully", response));
    }

    @GetMapping("/{paymentId}/status")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> getPaymentStatus(
            @PathVariable Long paymentId) {

        Long accountId = currentUserProvider.getCurrentUserId();
        PaymentStatusResponse response = paymentService.getPaymentStatus(accountId, paymentId);

        return ResponseEntity.ok(ApiResponse.success("Payment status retrieved successfully", response));
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<ApiResponse<Page<PaymentDetailResponse>>> getPaymentsByBooking(
            @PathVariable Long bookingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long accountId = currentUserProvider.getCurrentUserId();
        Page<PaymentDetailResponse> response = paymentService.getPaymentsByBooking(
                accountId, bookingId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        return ResponseEntity.ok(ApiResponse.success("Payment history retrieved successfully", response));
    }

    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<ApiResponse<CancelPaymentResponse>> cancelPayment(
            @PathVariable Long paymentId,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException("IDEMPOTENCY_KEY_REQUIRED",
                    "Idempotency-Key header is required", HttpStatus.BAD_REQUEST);
        }

        Long accountId = currentUserProvider.getCurrentUserId();
        CancelPaymentResponse response = paymentService.cancelPayment(accountId, idempotencyKey, paymentId);

        return ResponseEntity.ok(ApiResponse.success("Payment cancelled successfully", response));
    }
}
