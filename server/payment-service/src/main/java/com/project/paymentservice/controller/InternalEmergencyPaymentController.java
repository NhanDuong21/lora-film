package com.project.paymentservice.controller;

import com.project.paymentservice.common.ApiResponse;
import com.project.paymentservice.dto.request.EmergencyPaymentStopRequest;
import com.project.paymentservice.dto.response.EmergencyPaymentStopResponse;
import com.project.paymentservice.dto.response.EmergencyPaymentAssessmentResponse;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/internal/payments/emergency")
public class InternalEmergencyPaymentController {

    private final PaymentTransactionService paymentTransactionService;
    private final String triggerToken;
    private final String promotionAssessmentToken;

    public InternalEmergencyPaymentController(
            PaymentTransactionService paymentTransactionService,
            @Value("${payment.internal-trigger-token:}") String triggerToken,
            @Value("${payment.internal-promotion-assessment-token:}")
            String promotionAssessmentToken) {
        this.paymentTransactionService = paymentTransactionService;
        this.triggerToken = requireConfigured(
                triggerToken, "payment.internal-trigger-token");
        this.promotionAssessmentToken = requireConfigured(
                promotionAssessmentToken,
                "payment.internal-promotion-assessment-token");
    }

    @PostMapping("/stop")
    public ResponseEntity<ApiResponse<EmergencyPaymentStopResponse>> stopPendingPayments(
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @Valid @RequestBody EmergencyPaymentStopRequest request) {
        requireTriggerToken(token);
        PaymentTransactionService.EmergencyPaymentStopResult result =
                paymentTransactionService.stopActiveAttemptsForEmergency(
                        request.bookingPublicIds(), request.reason());
        return ResponseEntity.ok(ApiResponse.success(
                "Đã dừng các lần thanh toán còn chờ xử lý",
                new EmergencyPaymentStopResponse(
                        result.stoppedPaymentAttemptCount(),
                        result.alreadySuccessfulBookingPublicIds())));
    }

    @PostMapping("/assess")
    public ResponseEntity<ApiResponse<EmergencyPaymentAssessmentResponse>> assessPayments(
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @Valid @RequestBody EmergencyPaymentStopRequest request) {
        requireAssessmentToken(token);
        PaymentTransactionService.EmergencyPaymentAssessmentResult result =
                paymentTransactionService.assessPaymentsForEmergency(
                        request.bookingPublicIds());
        return ResponseEntity.ok(ApiResponse.success(
                "Đã kiểm tra trạng thái thanh toán mà không thay đổi dữ liệu",
                new EmergencyPaymentAssessmentResponse(
                        result.activePaymentBookingPublicIds(),
                        result.successfulPaymentBookingPublicIds())));
    }

    private void requireTriggerToken(String token) {
        if (!matches(token, triggerToken)) {
            throw new BusinessException(
                    "INTERNAL_TOKEN_INVALID",
                    "Xác thực nội bộ không hợp lệ",
                    HttpStatus.UNAUTHORIZED);
        }
    }

    private void requireAssessmentToken(String token) {
        if (!matches(token, triggerToken)
                && !matches(token, promotionAssessmentToken)) {
            throw new BusinessException(
                    "INTERNAL_TOKEN_INVALID",
                    "Xác thực nội bộ không hợp lệ",
                    HttpStatus.UNAUTHORIZED);
        }
    }

    private boolean matches(String actual, String expected) {
        if (expected == null || expected.isBlank() || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    private String requireConfigured(String value, String property) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "CRITICAL SECURITY FAILURE: '" + property
                            + "' must be configured");
        }
        return value;
    }
}
