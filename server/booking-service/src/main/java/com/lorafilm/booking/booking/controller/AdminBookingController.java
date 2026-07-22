package com.lorafilm.booking.booking.controller;

import com.lorafilm.booking.booking.dto.BookingAdminResponse;
import com.lorafilm.booking.booking.dto.BookingDetailResponse;
import com.lorafilm.booking.booking.dto.BookingFilterRequest;
import com.lorafilm.booking.booking.dto.UpdateBookingStatusRequest;
import com.lorafilm.booking.booking.service.AdminBookingService;
import com.lorafilm.booking.common.response.ApiResponse;
import com.lorafilm.booking.common.response.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/bookings")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Booking Management API", description = "Admin endpoints for listing, inspecting, and updating booking status")
public class AdminBookingController {

    private final AdminBookingService adminBookingService;

    public AdminBookingController(AdminBookingService adminBookingService) {
        this.adminBookingService = adminBookingService;
    }

    @GetMapping
    @Operation(summary = "Search and list bookings", description = "Filter bookings by bookingCode, userId, status, date range, with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<BookingAdminResponse>>> getBookings(@ModelAttribute BookingFilterRequest filter) {
        PagedResponse<BookingAdminResponse> response = adminBookingService.findBookings(filter);
        return ResponseEntity.ok(ApiResponse.success("Bookings retrieved successfully", response));
    }

    @GetMapping("/{bookingId}")
    @Operation(summary = "Get booking detail", description = "Retrieve full booking detail including snapshot, tickets, and status transition history")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> getBookingDetail(@PathVariable Long bookingId) {
        BookingDetailResponse detail = adminBookingService.getBookingDetail(bookingId);
        return ResponseEntity.ok(ApiResponse.success("Booking detail retrieved successfully", detail));
    }

    @PutMapping("/{bookingId}/status")
    @Operation(summary = "Update booking status", description = "Update status of a booking with validation, transition history, audit, operation log, and outbox event")
    public ResponseEntity<ApiResponse<BookingAdminResponse>> updateBookingStatus(
            @PathVariable Long bookingId,
            @Valid @RequestBody UpdateBookingStatusRequest request) {
        BookingAdminResponse updatedBooking = adminBookingService.updateBookingStatus(bookingId, request);
        return ResponseEntity.ok(ApiResponse.success("Booking status updated successfully", updatedBooking));
    }
}
