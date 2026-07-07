package com.project.bookingservice.controller;

import com.project.bookingservice.common.ApiResponse;
import com.project.bookingservice.dto.request.UpdateBookingStatusRequest;
import com.project.bookingservice.dto.response.BookingResponse;
import com.project.bookingservice.enumtype.BookingStatus;
import com.project.bookingservice.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/bookings")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
public class AdminBookingController {

    private final BookingService bookingService;

    public AdminBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> searchBookings(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long showtimeId,
            @RequestParam(required = false) String bookingCode,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo,
            Pageable pageable) {
        Page<BookingResponse> responses = bookingService.searchBookings(userId, showtimeId, bookingCode, status, createdFrom, createdTo, pageable);
        return ResponseEntity.ok(ApiResponse.success("Bookings retrieved successfully", responses));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(@PathVariable Long bookingId) {
        BookingResponse response = bookingService.getAdminBookingDetail(bookingId);
        return ResponseEntity.ok(ApiResponse.success("Booking retrieved successfully", response));
    }

    @PatchMapping("/{bookingId}/status")
    public ResponseEntity<ApiResponse<Void>> updateBookingStatus(
            @PathVariable Long bookingId,
            @Valid @RequestBody UpdateBookingStatusRequest request) {
        bookingService.updateBookingStatusAdmin(bookingId, request.getNewStatus(), request.getReason());
        return ResponseEntity.ok(ApiResponse.success("Booking status updated successfully", null));
    }
}
