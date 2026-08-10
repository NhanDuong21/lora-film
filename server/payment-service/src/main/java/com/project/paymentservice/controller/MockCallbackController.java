package com.project.paymentservice.controller;

import com.project.paymentservice.common.ApiResponse;
import com.project.paymentservice.dto.request.MockCallbackRequest;
import com.project.paymentservice.security.CurrentUserProvider;
import com.project.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/mock")
@ConditionalOnProperty(name = "payment.providers.mock.enabled", havingValue = "true")
public class MockCallbackController {
    private final PaymentService paymentService;
    private final CurrentUserProvider currentUserProvider;

    public MockCallbackController(
            PaymentService paymentService, CurrentUserProvider currentUserProvider) {
        this.paymentService = paymentService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/{paymentPublicId}/complete")
    public ResponseEntity<ApiResponse<String>> complete(
            @PathVariable String paymentPublicId,
            @Valid @RequestBody MockCallbackRequest request) {
        paymentService.processMockCallback(
                currentUserProvider.getCurrentUserId(),
                paymentPublicId,
                request.getSimulatedStatus());
        return ResponseEntity.ok(ApiResponse.success("Đã xử lý kết quả MOCK", "OK"));
    }
}
