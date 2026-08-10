package com.project.paymentservice.controller;

import com.project.paymentservice.common.ApiResponse;
import com.project.paymentservice.dto.request.EmergencyPaymentStopRequest;
import com.project.paymentservice.dto.response.EmergencyPaymentStopResponse;
import com.project.paymentservice.exception.BusinessException;
import com.project.paymentservice.service.PaymentTransactionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/payments/emergency")
public class InternalEmergencyPaymentController {

    private final PaymentTransactionService paymentTransactionService;
    private final String internalToken;

    public InternalEmergencyPaymentController(
            PaymentTransactionService paymentTransactionService,
            @Value("${payment.internal-trigger-token:}") String internalToken) {
        this.paymentTransactionService = paymentTransactionService;
        this.internalToken = internalToken;
    }

    @PostMapping("/stop")
    public ResponseEntity<ApiResponse<EmergencyPaymentStopResponse>> stopPendingPayments(
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @Valid @RequestBody EmergencyPaymentStopRequest request) {
        requireInternalToken(token);
        PaymentTransactionService.EmergencyPaymentStopResult result =
                paymentTransactionService.stopActiveAttemptsForEmergency(
                        request.bookingPublicIds(), request.reason());
        return ResponseEntity.ok(ApiResponse.success(
                "Đã dừng các lần thanh toán còn chờ xử lý",
                new EmergencyPaymentStopResponse(
                        result.stoppedPaymentAttemptCount(),
                        result.alreadySuccessfulBookingPublicIds())));
    }

    private void requireInternalToken(String token) {
        if (internalToken == null || internalToken.isBlank()
                || token == null || !internalToken.equals(token)) {
            throw new BusinessException(
                    "INTERNAL_TOKEN_INVALID",
                    "Xác thực nội bộ không hợp lệ",
                    HttpStatus.UNAUTHORIZED);
        }
    }
}
