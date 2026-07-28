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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
@RequestMapping("/api/payments")
@Tag(name = "Payment", description = "Customer Payment API using public UUIDs")
@SecurityRequirement(name = "bearerAuth")
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
            @Valid @RequestBody CreatePaymentRequest request,
            HttpServletRequest servletRequest) {
        requireKey(idempotencyKey);
        CreatePaymentResponse response = paymentService.createPayment(
                currentUserProvider.getCurrentUserId(),
                idempotencyKey,
                request,
                clientIp(servletRequest));
        HttpStatus status = response.getPaymentUrl() == null
                && "PROCESSING".equals(response.getStatus())
                ? HttpStatus.ACCEPTED : HttpStatus.CREATED;
        return ResponseEntity.status(status)
                .body(ApiResponse.success("Đã khởi tạo giao dịch thanh toán", response));
    }

    @GetMapping("/{paymentPublicId:[a-fA-F0-9-]{36}}")
    public ResponseEntity<ApiResponse<PaymentDetailResponse>> getPayment(
            @PathVariable String paymentPublicId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPayment(
                currentUserProvider.getCurrentUserId(), paymentPublicId)));
    }

    @GetMapping("/{paymentPublicId:[a-fA-F0-9-]{36}}/status")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> getPaymentStatus(
            @PathVariable String paymentPublicId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentStatus(
                currentUserProvider.getCurrentUserId(), paymentPublicId)));
    }

    @GetMapping("/booking/{bookingPublicId:[a-fA-F0-9-]{36}}")
    public ResponseEntity<ApiResponse<Page<PaymentDetailResponse>>> getPaymentsByBooking(
            @PathVariable String bookingPublicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentsByBooking(
                currentUserProvider.getCurrentUserId(), bookingPublicId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))));
    }

    @PostMapping("/{paymentPublicId:[a-fA-F0-9-]{36}}/cancel")
    public ResponseEntity<ApiResponse<CancelPaymentResponse>> cancelPayment(
            @PathVariable String paymentPublicId,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        requireKey(idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(
                "Đã hủy giao dịch thanh toán",
                paymentService.cancelPayment(currentUserProvider.getCurrentUserId(),
                        idempotencyKey, paymentPublicId)));
    }

    /** Deprecated numeric compatibility routes. */
    @GetMapping("/{paymentId:\\d+}")
    public ResponseEntity<ApiResponse<PaymentDetailResponse>> getPaymentCompat(
            @PathVariable Long paymentId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPayment(
                currentUserProvider.getCurrentUserId(), paymentId)));
    }

    @GetMapping("/{paymentId:\\d+}/status")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> getPaymentStatusCompat(
            @PathVariable Long paymentId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentStatus(
                currentUserProvider.getCurrentUserId(), paymentId)));
    }

    @GetMapping("/booking/{bookingId:\\d+}")
    public ResponseEntity<ApiResponse<Page<PaymentDetailResponse>>> getByBookingCompat(
            @PathVariable Long bookingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentsByBooking(
                currentUserProvider.getCurrentUserId(), bookingId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))));
    }

    @PostMapping("/{paymentId:\\d+}/cancel")
    public ResponseEntity<ApiResponse<CancelPaymentResponse>> cancelCompat(
            @PathVariable Long paymentId,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        requireKey(idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(paymentService.cancelPayment(
                currentUserProvider.getCurrentUserId(), idempotencyKey, paymentId)));
    }

    private void requireKey(String value) {
        if (value == null || value.isBlank() || value.length() > 100) {
            throw new BusinessException("IDEMPOTENCY_KEY_REQUIRED",
                    "Idempotency-Key là bắt buộc và tối đa 100 ký tự",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null || forwarded.isBlank()
                ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
    }
}
