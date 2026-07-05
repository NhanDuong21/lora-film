package com.project.bookingservice.controller;

import com.project.bookingservice.api.InternalBookingApi;
import com.project.bookingservice.common.ApiResponse;
import com.project.bookingservice.dto.payment.ConfirmPaymentRequest;
import com.project.bookingservice.dto.payment.ConfirmPaymentResponse;
import com.project.bookingservice.dto.payment.FailPaymentRequest;
import com.project.bookingservice.service.InternalPaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/bookings")
public class InternalBookingController implements InternalBookingApi {

    private final InternalPaymentService internalPaymentService;

    public InternalBookingController(InternalPaymentService internalPaymentService) {
        this.internalPaymentService = internalPaymentService;
    }

    @Override
    @PostMapping("/{bookingId}/confirm-payment")
    public ResponseEntity<ApiResponse<ConfirmPaymentResponse>> confirmPayment(
            @PathVariable Long bookingId,
            @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @Valid @RequestBody ConfirmPaymentRequest request) {
        ConfirmPaymentResponse response = internalPaymentService.confirmPayment(bookingId, request, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success("Booking confirmed successfully", response));
    }

    @Override
    @PostMapping("/{bookingId}/fail-payment")
    public ResponseEntity<ApiResponse<Void>> failPayment(
            @PathVariable Long bookingId,
            @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @Valid @RequestBody FailPaymentRequest request) {
        internalPaymentService.failPayment(bookingId, request, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success("Payment failure logged successfully", null));
    }
}
