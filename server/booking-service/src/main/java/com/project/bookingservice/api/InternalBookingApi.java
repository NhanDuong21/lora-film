package com.project.bookingservice.api;

import com.project.bookingservice.common.ApiResponse;
import com.project.bookingservice.dto.payment.PaymentContextResponse;
import com.project.bookingservice.dto.payment.PaymentResultRequest;
import com.project.bookingservice.dto.payment.PaymentResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Internal Booking Payment Integration API", description = "Endpoints for payment service to communicate with booking service")
public interface InternalBookingApi {

    @Operation(summary = "Get Payment Context", description = "Provide Payment Service with an authoritative immutable snapshot before Payment Attempt creation")
    @GetMapping("/{bookingId}/payment-context")
    ResponseEntity<ApiResponse<PaymentContextResponse>> getPaymentContext(
            @PathVariable @Positive(message = "Booking ID must be greater than 0") Long bookingId,
            @RequestHeader(value = "X-Internal-Token", required = true) String internalToken);

    @Operation(summary = "Process Payment Results", description = "Handles SUCCESS, FAILED, CANCELLED, EXPIRED payment results")
    @PostMapping("/{bookingId}/confirm-payment")
    ResponseEntity<ApiResponse<PaymentResultResponse>> processPaymentResult(
            @PathVariable @Positive(message = "Booking ID must be greater than 0") Long bookingId,
            @RequestHeader(value = "X-Internal-Token", required = true) String internalToken,
            @Valid @RequestBody PaymentResultRequest request);
}
