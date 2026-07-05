package com.project.bookingservice.controller;

import com.project.bookingservice.api.BookingApi;
import com.project.bookingservice.common.ApiResponse;
import com.project.bookingservice.dto.request.CreateBookingRequest;
import com.project.bookingservice.dto.response.BookingResponse;
import com.project.bookingservice.enumtype.BookingStatus;
import com.project.bookingservice.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/bookings")
public class BookingController implements BookingApi {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @Override
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @Valid @RequestBody CreateBookingRequest request) {
        BookingResponse response = bookingService.createBooking(request, idempotencyKey);
        return new ResponseEntity<>(ApiResponse.success("Booking created successfully", response), HttpStatus.CREATED);
    }

    @GetMapping("/{bookingId}")
    @Override
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(@PathVariable Long bookingId) {
        BookingResponse response = bookingService.getBooking(bookingId);
        return ResponseEntity.ok(ApiResponse.success("Booking retrieved successfully", response));
    }

    @GetMapping("/me")
    @Override
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getMyBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Pageable pageable) {
        Page<BookingResponse> responses = bookingService.getMyBookings(status, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.success("Bookings retrieved successfully", responses));
    }

    @PostMapping("/{bookingId}/cancel")
    @Override
    public ResponseEntity<ApiResponse<Void>> cancelBooking(
            @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @PathVariable Long bookingId) {
        bookingService.cancelBooking(bookingId, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully", null));
    }
}
