package com.project.paymentservice.controller;

import com.project.paymentservice.common.ApiResponse;
import com.project.paymentservice.dto.request.CashCancelRequest;
import com.project.paymentservice.dto.request.CashCollectRequest;
import com.project.paymentservice.dto.response.CashCancelResponse;
import com.project.paymentservice.dto.response.CashCollectResponse;
import com.project.paymentservice.exception.BusinessException;
import com.project.paymentservice.security.CurrentUserProvider;
import com.project.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employee/payments")
@Tag(name = "Employee Payment", description = "Employee and Admin APIs for managing payments")
@SecurityRequirement(name = "bearerAuth")
public class EmployeePaymentController {

    private final PaymentService paymentService;
    private final CurrentUserProvider currentUserProvider;

    public EmployeePaymentController(PaymentService paymentService, CurrentUserProvider currentUserProvider) {
        this.paymentService = paymentService;
        this.currentUserProvider = currentUserProvider;
    }

    @Operation(summary = "Collect Cash", description = "Admin only (temporarily) - Collect cash at counter")
    @PostMapping("/{paymentId}/cash/collect")
    public ResponseEntity<ApiResponse<CashCollectResponse>> collectCash(
            @Parameter(description = "Payment ID", required = true) @PathVariable Long paymentId,
            @Parameter(description = "Required idempotency key", required = true)
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CashCollectRequest request) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException("IDEMPOTENCY_KEY_REQUIRED",
                    "Idempotency-Key header is required", HttpStatus.BAD_REQUEST);
        }

        Long accountId = currentUserProvider.getCurrentUserId();
        CashCollectResponse response = paymentService.collectCashPayment(accountId, idempotencyKey, paymentId, request);
        if ("PENDING".equals(response.getBookingDeliveryStatus())) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(ApiResponse.success("Cash payment collected successfully; booking confirmation is pending delivery", response));
        }
        return ResponseEntity.ok(ApiResponse.success("Cash payment collected successfully", response));
    }

    @Operation(summary = "Cancel Cash Payment", description = "Admin only (temporarily) - Cancel a pending cash payment")
    @PostMapping("/{paymentId}/cash/cancel")
    public ResponseEntity<ApiResponse<CashCancelResponse>> cancelCash(
            @Parameter(description = "Payment ID", required = true) @PathVariable Long paymentId,
            @Parameter(description = "Required idempotency key", required = true)
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CashCancelRequest request) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException("IDEMPOTENCY_KEY_REQUIRED",
                    "Idempotency-Key header is required", HttpStatus.BAD_REQUEST);
        }
        Long accountId = currentUserProvider.getCurrentUserId();
        CashCancelResponse response = paymentService.cancelCashPayment(accountId, idempotencyKey, paymentId, request);
        if ("PENDING".equals(response.getBookingDeliveryStatus())) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(ApiResponse.success("Cash payment cancelled successfully; booking confirmation is pending delivery", response));
        }
        return ResponseEntity.ok(ApiResponse.success("Cash payment cancelled successfully", response));
    }
}
