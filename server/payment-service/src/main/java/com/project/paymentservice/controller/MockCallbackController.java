package com.project.paymentservice.controller;

import com.project.paymentservice.common.ApiResponse;
import com.project.paymentservice.dto.request.MockCallbackRequest;
import com.project.paymentservice.exception.BusinessException;
import com.project.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/payments/callback")
@ConditionalOnProperty(name = "payment.mock.enabled", havingValue = "true", matchIfMissing = false)
@Tag(name = "MOCK Callback", description = "LOCAL/TEST ONLY. Unavailable when payment.mock.enabled=false. Must never be enabled in production.")
public class MockCallbackController {

    private final PaymentService paymentService;

    public MockCallbackController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(summary = "Handle MOCK Payment Callback", description = "Simulates a payment provider callback. LOCAL/TEST ONLY.")
    @PostMapping("/mock")
    public ResponseEntity<ApiResponse<String>> handleMockCallback(
            @Valid @RequestBody MockCallbackRequest request) {

        try {
            paymentService.processMockCallback(request);
            return ResponseEntity.ok(ApiResponse.success("MOCK callback processed", "OK"));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("INTERNAL_SERVER_ERROR",
                    "Failed to process MOCK callback", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
