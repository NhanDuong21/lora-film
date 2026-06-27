package com.project.bookingservice.controller;

import com.project.bookingservice.common.ApiResponse;
import com.project.bookingservice.dto.reservation.CreateReservationRequest;
import com.project.bookingservice.dto.reservation.ReservationGroupResponse;
import com.project.bookingservice.dto.reservation.ReservationResponse;
import com.project.bookingservice.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings/seat-reservations")
public class SeatReservationController {

    private final ReservationService reservationService;

    public SeatReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @Operation(summary = "Create a new seat reservation", description = "Reserves one or more seats for a showtime atomically. Requires Idempotency-Key header.")
    public ResponseEntity<ApiResponse<ReservationGroupResponse>> createReservation(
            @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @Valid @RequestBody CreateReservationRequest request) {

        ReservationGroupResponse responses = reservationService.createReservation(request, idempotencyKey);
        return new ResponseEntity<>(ApiResponse.success("Seats reserved successfully", responses), HttpStatus.CREATED);
    }

    @GetMapping("/{reservationId}")
    @Operation(summary = "Get a seat reservation", description = "Retrieve details of a specific seat reservation by ID. User must own the reservation.")
    public ResponseEntity<ApiResponse<ReservationResponse>> getReservation(@PathVariable Long reservationId) {
        ReservationResponse response = reservationService.getReservation(reservationId);
        return ResponseEntity.ok(ApiResponse.success("Seat reservation retrieved successfully", response));
    }

    @DeleteMapping("/{reservationId}")
    @Operation(summary = "Release a seat reservation", description = "Release a held seat reservation. Idempotent operation. User must own the reservation.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> releaseReservation(@PathVariable Long reservationId) {
        reservationService.releaseReservation(reservationId);
        Map<String, Object> data = new HashMap<>();
        data.put("reservationId", reservationId);
        data.put("status", "RELEASED");
        return ResponseEntity.ok(ApiResponse.success("Seat reservation released successfully", data));
    }
}
