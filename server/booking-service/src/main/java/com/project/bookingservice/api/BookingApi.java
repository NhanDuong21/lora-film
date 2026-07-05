package com.project.bookingservice.api;

import com.project.bookingservice.common.ApiResponse;
import com.project.bookingservice.dto.request.CreateBookingRequest;
import com.project.bookingservice.dto.response.BookingResponse;
import com.project.bookingservice.enumtype.BookingStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

@Tag(name = "Booking", description = "Customer Booking APIs")
public interface BookingApi {

    @Operation(summary = "Create a booking")
    ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @Valid @RequestBody CreateBookingRequest request);

    @Operation(summary = "Get booking detail")
    ResponseEntity<ApiResponse<BookingResponse>> getBooking(
            @PathVariable Long bookingId);

    @Operation(summary = "Get my bookings")
    ResponseEntity<ApiResponse<Page<BookingResponse>>> getMyBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            @Parameter(hidden = true) Pageable pageable);

    @Operation(summary = "Cancel a booking")
    ResponseEntity<ApiResponse<Void>> cancelBooking(
            @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @PathVariable Long bookingId);
}
