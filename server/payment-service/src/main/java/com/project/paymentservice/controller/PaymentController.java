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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payment", description = "Payment Core API")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;
    private final CurrentUserProvider currentUserProvider;

    public PaymentController(PaymentService paymentService, CurrentUserProvider currentUserProvider) {
        this.paymentService = paymentService;
        this.currentUserProvider = currentUserProvider;
    }

    @Operation(summary = "Create Payment", description = "Idempotent payment creation")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Payment created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Idempotency conflict or Invalid state")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<CreatePaymentResponse>> createPayment(
            @Parameter(description = "Required, unique per logical operation. Reuse with the same request returns deterministic replay. Reuse with different request data returns IDEMPOTENCY_KEY_REUSED.", required = true, example = "payment-create-manual-001")
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

    @Operation(summary = "Get Payment details", description = "Retrieve a specific payment by its ID")
    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentDetailResponse>> getPayment(
            @Parameter(description = "Payment ID", required = true) @PathVariable Long paymentId) {

        Long accountId = currentUserProvider.getCurrentUserId();
        PaymentDetailResponse response = paymentService.getPayment(accountId, paymentId);

        return ResponseEntity.ok(ApiResponse.success("Payment retrieved successfully", response));
    }

    @Operation(summary = "Get Payment Status", description = "Retrieve only the status of a specific payment")
    @GetMapping("/{paymentId}/status")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> getPaymentStatus(
            @Parameter(description = "Payment ID", required = true) @PathVariable Long paymentId) {

        Long accountId = currentUserProvider.getCurrentUserId();
        PaymentStatusResponse response = paymentService.getPaymentStatus(accountId, paymentId);

        return ResponseEntity.ok(ApiResponse.success("Payment status retrieved successfully", response));
    }

    @Operation(summary = "List Payments by Booking", description = "Retrieve a paginated list of payments for a booking")
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<ApiResponse<Page<PaymentDetailResponse>>> getPaymentsByBooking(
            @Parameter(description = "Booking ID", required = true) @PathVariable Long bookingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long accountId = currentUserProvider.getCurrentUserId();
        Page<PaymentDetailResponse> response = paymentService.getPaymentsByBooking(
                accountId, bookingId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        return ResponseEntity.ok(ApiResponse.success("Payment history retrieved successfully", response));
    }

    @Operation(summary = "Cancel Payment", description = "Idempotent payment cancellation")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment cancelled"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Idempotency conflict or Invalid state")
    })
    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<ApiResponse<CancelPaymentResponse>> cancelPayment(
            @Parameter(description = "Payment ID", required = true) @PathVariable Long paymentId,
            @Parameter(description = "Required, unique per logical operation. Reuse with the same request returns deterministic replay. Reuse with different request data returns IDEMPOTENCY_KEY_REUSED.", required = true, example = "payment-cancel-manual-001")
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
