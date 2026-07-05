package com.project.bookingservice.controller;

import com.project.bookingservice.api.InternalBookingApi;
import com.project.bookingservice.common.ApiResponse;
import com.project.bookingservice.dto.payment.PaymentContextResponse;
import com.project.bookingservice.dto.payment.PaymentResultRequest;
import com.project.bookingservice.dto.payment.PaymentResultResponse;
import com.project.bookingservice.exception.BusinessException;
import com.project.bookingservice.service.InternalPaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/bookings")
public class InternalBookingController implements InternalBookingApi {

    private final InternalPaymentService internalPaymentService;
    private final String internalTokenConfig;

    public InternalBookingController(
            InternalPaymentService internalPaymentService,
            @Value("${internal.api.token:default-secret-token}") String internalTokenConfig) {
        this.internalPaymentService = internalPaymentService;
        this.internalTokenConfig = internalTokenConfig;
    }

    private void validateToken(String token) {
        if (!internalTokenConfig.equals(token)) {
            throw new BusinessException("INTERNAL_TOKEN_INVALID", "Invalid internal token");
        }
    }

    @Override
    @GetMapping("/{bookingId}/payment-context")
    public ResponseEntity<ApiResponse<PaymentContextResponse>> getPaymentContext(
            @PathVariable @Positive(message = "Booking ID must be greater than 0") Long bookingId,
            @RequestHeader(value = "X-Internal-Token", required = true) String internalToken) {
        
        validateToken(internalToken);
        PaymentContextResponse response = internalPaymentService.getPaymentContext(bookingId);
        return ResponseEntity.ok(ApiResponse.success("Booking payment context retrieved successfully", response));
    }

    @Override
    @PostMapping("/{bookingId}/confirm-payment")
    public ResponseEntity<ApiResponse<PaymentResultResponse>> processPaymentResult(
            @PathVariable @Positive(message = "Booking ID must be greater than 0") Long bookingId,
            @RequestHeader(value = "X-Internal-Token", required = true) String internalToken,
            @Valid @RequestBody PaymentResultRequest request) {
        
        validateToken(internalToken);
        PaymentResultResponse response = internalPaymentService.processPaymentResult(bookingId, request);
        
        if (response.isDuplicate()) {
            return ResponseEntity.ok(ApiResponse.success("Payment result already processed", response));
        } else if (!response.isApplied() && "ALREADY_CONFIRMED_BY_ANOTHER_PAYMENT".equals(response.getResult())) {
            return ResponseEntity.ok(ApiResponse.success("Payment result acknowledged but not applied", response));
        }
        
        return ResponseEntity.ok(ApiResponse.success("Payment result applied successfully", response));
    }
}
