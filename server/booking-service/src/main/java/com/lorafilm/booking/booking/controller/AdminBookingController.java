package com.lorafilm.booking.booking.controller;

import com.lorafilm.booking.booking.dto.BookingAdminResponse;
import com.lorafilm.booking.booking.dto.BookingDetailResponse;
import com.lorafilm.booking.booking.dto.BookingFilterRequest;
import com.lorafilm.booking.booking.dto.UpdateBookingStatusRequest;
import com.lorafilm.booking.booking.dto.BookingOperationsSummaryResponse;
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

    @GetMapping("/summary")
    @Operation(summary = "Get global booking operations summary",
            description = "Global lifecycle and attention counters backed by Booking Service data")
    public ResponseEntity<ApiResponse<BookingOperationsSummaryResponse>> getOperationsSummary() {
        return ResponseEntity.ok(ApiResponse.success(
                "Booking operations summary retrieved successfully",
                adminBookingService.getOperationsSummary()));
    }

    @GetMapping("/{publicId:[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}}")
    @Operation(summary = "Get booking detail", description = "Retrieve full booking detail including snapshot, tickets, and status transition history using public UUID")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> getBookingDetail(@PathVariable String publicId) {
        BookingDetailResponse detail = adminBookingService.getBookingDetail(publicId);
        return ResponseEntity.ok(ApiResponse.success("Booking detail retrieved successfully", detail));
    }

    @PutMapping("/{publicId:[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}}/status")
    @Operation(summary = "Update booking status", description = "Update status of a booking with validation, transition history, audit, operation log, and outbox event using public UUID")
    public ResponseEntity<ApiResponse<BookingAdminResponse>> updateBookingStatus(
            @PathVariable String publicId,
            @Valid @RequestBody UpdateBookingStatusRequest request) {
        BookingAdminResponse updatedBooking = adminBookingService.updateBookingStatus(publicId, request);
        return ResponseEntity.ok(ApiResponse.success("Booking status updated successfully", updatedBooking));
    }
}
