package com.project.bookingservice.api;

import com.project.bookingservice.common.ApiResponse;
import com.project.bookingservice.dto.payment.ConfirmPaymentRequest;
import com.project.bookingservice.dto.payment.ConfirmPaymentResponse;
import com.project.bookingservice.dto.payment.FailPaymentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Internal Booking Payment Integration API", description = "Endpoints for payment service to communicate with booking service")
public interface InternalBookingApi {

    @Operation(summary = "Confirm Payment", description = "Confirm booking payment and generate tickets")
    ResponseEntity<ApiResponse<ConfirmPaymentResponse>> confirmPayment(
            @PathVariable Long bookingId,
            @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @Valid @RequestBody ConfirmPaymentRequest request);

    @Operation(summary = "Fail Payment", description = "Fail booking payment and allow retry if not expired")
    ResponseEntity<ApiResponse<Void>> failPayment(
            @PathVariable Long bookingId,
            @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @Valid @RequestBody FailPaymentRequest request);
}
