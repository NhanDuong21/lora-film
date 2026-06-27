package com.project.bookingservice.controller;

import com.project.bookingservice.dto.reservation.CreateReservationRequest;
import com.project.bookingservice.dto.reservation.ReservationResponse;
import com.project.bookingservice.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.project.bookingservice.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/bookings/seat-reservations")
@Tag(name = "Seat Reservations", description = "Endpoints for managing seat reservations")
public class SeatReservationController {

    private final ReservationService reservationService;

    public SeatReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @Operation(summary = "Create a new seat reservation", description = "Reserves one or more seats for a showtime atomically. Requires Idempotency-Key header.")
    public ResponseEntity<ApiResponse<List<ReservationResponse>>> createReservation(
            @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @Valid @RequestBody CreateReservationRequest request) {

        List<ReservationResponse> responses = reservationService.createReservation(request, idempotencyKey);
        return new ResponseEntity<>(ApiResponse.success(responses), HttpStatus.CREATED);
    }

    @GetMapping("/{reservationId}")
    @Operation(summary = "Get a seat reservation", description = "Retrieve details of a specific seat reservation by ID. User must own the reservation.")
    public ResponseEntity<ApiResponse<ReservationResponse>> getReservation(@PathVariable Long reservationId) {
        ReservationResponse response = reservationService.getReservation(reservationId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{reservationId}")
    @Operation(summary = "Release a seat reservation", description = "Release a held seat reservation. Idempotent operation. User must own the reservation.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> releaseReservation(@PathVariable Long reservationId) {
        reservationService.releaseReservation(reservationId);
        Map<String, Object> data = new HashMap<>();
        data.put("reservationId", reservationId);
        data.put("status", "RELEASED");
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
