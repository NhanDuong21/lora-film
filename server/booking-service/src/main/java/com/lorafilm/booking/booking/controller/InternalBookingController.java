package com.lorafilm.booking.booking.controller;

import com.lorafilm.booking.booking.dto.BookingAdminResponse;
import com.lorafilm.booking.booking.service.InternalBookingService;
import com.lorafilm.booking.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/bookings")
@Tag(name = "Internal Booking API", description = "Inter-service communication endpoints for confirmation, expiration, refund, and retrieval")
public class InternalBookingController {

    private final InternalBookingService internalBookingService;

    public InternalBookingController(InternalBookingService internalBookingService) {
        this.internalBookingService = internalBookingService;
    }

    @PostMapping("/{bookingId}/confirm")
    @Operation(summary = "Internal confirm booking", description = "Confirm booking payment internally")
    public ResponseEntity<ApiResponse<BookingAdminResponse>> confirmBooking(@PathVariable Long bookingId) {
        BookingAdminResponse response = internalBookingService.confirmBooking(bookingId);
        return ResponseEntity.ok(ApiResponse.success("Booking confirmed successfully", response));
    }

    @PostMapping("/{bookingId}/expire")
    @Operation(summary = "Internal expire booking", description = "Expire booking due to payment timeout internally")
    public ResponseEntity<ApiResponse<BookingAdminResponse>> expireBooking(@PathVariable Long bookingId) {
        BookingAdminResponse response = internalBookingService.expireBooking(bookingId);
        return ResponseEntity.ok(ApiResponse.success("Booking expired successfully", response));
    }

    @PostMapping("/{bookingId}/refund")
    @Operation(summary = "Internal refund booking", description = "Process refund for booking internally")
    public ResponseEntity<ApiResponse<BookingAdminResponse>> refundBooking(@PathVariable Long bookingId) {
        BookingAdminResponse response = internalBookingService.refundBooking(bookingId);
        return ResponseEntity.ok(ApiResponse.success("Booking refunded successfully", response));
    }

    @GetMapping("/code/{bookingCode}")
    @Operation(summary = "Internal get booking by code", description = "Retrieve booking information using booking code internally")
    public ResponseEntity<ApiResponse<BookingAdminResponse>> getBookingByCode(@PathVariable String bookingCode) {
        BookingAdminResponse response = internalBookingService.getBookingByCode(bookingCode);
        return ResponseEntity.ok(ApiResponse.success("Booking retrieved successfully", response));
    }
}
