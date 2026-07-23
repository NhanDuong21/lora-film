package com.lorafilm.booking.booking.controller;

import com.lorafilm.booking.booking.dto.BookingAdminResponse;
import com.lorafilm.booking.booking.dto.BookingPaymentContextDto;
import com.lorafilm.booking.booking.dto.BookingPaymentResultRequestDto;
import com.lorafilm.booking.booking.dto.BookingPaymentResultResponseDto;
import com.lorafilm.booking.booking.service.InternalBookingService;
import com.lorafilm.booking.common.constant.ValidationConstants;
import com.lorafilm.booking.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/bookings")
@Validated
@Tag(name = "Internal Booking API", description = "Inter-service communication endpoints for confirmation, expiration, refund, and retrieval")
@SecurityRequirement(name = "internalTokenAuth")
public class InternalBookingController {

    private final InternalBookingService internalBookingService;

    public InternalBookingController(InternalBookingService internalBookingService) {
        this.internalBookingService = internalBookingService;
    }

    @PostMapping("/{publicId:[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}}/confirm")
    @Operation(summary = "Confirm a pending booking using public UUID")
    public ResponseEntity<ApiResponse<BookingAdminResponse>> confirmBooking(
            @PathVariable
            @Parameter(description = "Booking publicId (UUID), not the internal database id",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @Pattern(regexp = ValidationConstants.UUID_PATTERN, message = "publicId must be a valid UUID")
            String publicId) {
        BookingAdminResponse response = internalBookingService.confirmBooking(publicId);
        return ResponseEntity.ok(ApiResponse.success("Booking confirmed successfully", response));
    }

    @PostMapping("/{publicId:[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}}/expire")
    @Operation(summary = "Expire a booking after its payment deadline using public UUID")
    public ResponseEntity<ApiResponse<BookingAdminResponse>> expireBooking(
            @PathVariable
            @Parameter(description = "Booking publicId (UUID), not the internal database id",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @Pattern(regexp = ValidationConstants.UUID_PATTERN, message = "publicId must be a valid UUID")
            String publicId) {
        BookingAdminResponse response = internalBookingService.expireBooking(publicId);
        return ResponseEntity.ok(ApiResponse.success("Booking expired successfully", response));
    }

    @PostMapping("/{publicId:[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}}/refund")
    @Operation(summary = "Mark a confirmed booking as refunded using public UUID")
    public ResponseEntity<ApiResponse<BookingAdminResponse>> refundBooking(
            @PathVariable
            @Parameter(description = "Booking publicId (UUID), not the internal database id",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @Pattern(regexp = ValidationConstants.UUID_PATTERN, message = "publicId must be a valid UUID")
            String publicId) {
        BookingAdminResponse response = internalBookingService.refundBooking(publicId);
        return ResponseEntity.ok(ApiResponse.success("Booking refunded successfully", response));
    }

    @GetMapping("/code/{bookingCode}")
    @Operation(summary = "Internal get booking by code", description = "Retrieve booking information using booking code internally")
    public ResponseEntity<ApiResponse<BookingAdminResponse>> getBookingByCode(@PathVariable String bookingCode) {
        BookingAdminResponse response = internalBookingService.getBookingByCode(bookingCode);
        return ResponseEntity.ok(ApiResponse.success("Booking retrieved successfully", response));
    }

    @GetMapping("/{bookingId:\\d+}/payment-context")
    @Operation(summary = "Get payment context of booking")
    public ResponseEntity<ApiResponse<BookingPaymentContextDto>> getPaymentContext(@PathVariable Long bookingId) {
        BookingPaymentContextDto response = internalBookingService.getPaymentContext(bookingId);
        return ResponseEntity.ok(ApiResponse.success("Booking payment context retrieved successfully", response));
    }

    @PostMapping("/{bookingId:\\d+}/payment-results")
    @Operation(summary = "Process payment results of booking")
    public ResponseEntity<ApiResponse<BookingPaymentResultResponseDto>> processPaymentResult(
            @PathVariable Long bookingId,
            @Valid @RequestBody BookingPaymentResultRequestDto request) {
        BookingPaymentResultResponseDto response = internalBookingService.processPaymentResult(bookingId, request);
        return ResponseEntity.ok(ApiResponse.success("Payment result processed successfully", response));
    }
}
