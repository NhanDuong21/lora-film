package com.lorafilm.booking.booking.controller;

import com.lorafilm.booking.booking.dto.response.BookingResponse;
import com.lorafilm.booking.booking.service.BookingService;
import com.lorafilm.booking.common.constant.ValidationConstants;
import com.lorafilm.booking.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/bookings")
@Validated
@Tag(name = "Internal Booking API", description = "Booking lifecycle operations for trusted services")
@SecurityRequirement(name = "internalTokenAuth")
public class InternalBookingController {

    private final BookingService bookingService;

    public InternalBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/{publicId}/confirm")
    @Operation(summary = "Confirm a pending booking")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(
            @PathVariable
            @Parameter(description = "Booking publicId (UUID), not the internal database id",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @Pattern(regexp = ValidationConstants.UUID_PATTERN, message = "publicId must be a valid UUID")
            String publicId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Booking confirmed successfully", bookingService.confirmBooking(publicId)));
    }

    @PostMapping("/{publicId}/expire")
    @Operation(summary = "Expire a booking after its payment deadline")
    public ResponseEntity<ApiResponse<BookingResponse>> expireBooking(
            @PathVariable
            @Parameter(description = "Booking publicId (UUID), not the internal database id",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @Pattern(regexp = ValidationConstants.UUID_PATTERN, message = "publicId must be a valid UUID")
            String publicId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Booking expired successfully", bookingService.expireBooking(publicId)));
    }

    @PostMapping("/{publicId}/refund")
    @Operation(summary = "Mark a confirmed booking as refunded")
    public ResponseEntity<ApiResponse<BookingResponse>> refundBooking(
            @PathVariable
            @Parameter(description = "Booking publicId (UUID), not the internal database id",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @Pattern(regexp = ValidationConstants.UUID_PATTERN, message = "publicId must be a valid UUID")
            String publicId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Booking refunded successfully", bookingService.refundBooking(publicId)));
    }
}
