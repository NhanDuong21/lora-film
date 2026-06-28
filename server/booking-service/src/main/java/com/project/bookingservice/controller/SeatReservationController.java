package com.project.bookingservice.controller;

import com.project.bookingservice.api.SeatReservationApi;
import com.project.bookingservice.common.ApiResponse;
import com.project.bookingservice.dto.reservation.CreateReservationRequest;
import com.project.bookingservice.dto.reservation.ReservationGroupResponse;
import com.project.bookingservice.dto.reservation.ReservationResponse;
import com.project.bookingservice.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings/seat-reservations")
public class SeatReservationController implements SeatReservationApi {

    private final ReservationService reservationService;

    public SeatReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @Override
    public ResponseEntity<ApiResponse<ReservationGroupResponse>> createReservation(
            @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @Valid @RequestBody CreateReservationRequest request) {

        ReservationGroupResponse responses = reservationService.createReservation(request, idempotencyKey);
        return new ResponseEntity<>(ApiResponse.success("Seats reserved successfully", responses), HttpStatus.CREATED);
    }

    @GetMapping("/{reservationId}")
    @Override
    public ResponseEntity<ApiResponse<ReservationResponse>> getReservation(@PathVariable Long reservationId) {
        ReservationResponse response = reservationService.getReservation(reservationId);
        return ResponseEntity.ok(ApiResponse.success("Seat reservation retrieved successfully", response));
    }

    @DeleteMapping("/{reservationId}")
    @Override
    public ResponseEntity<ApiResponse<com.project.bookingservice.dto.reservation.ReleaseReservationResponse>> releaseReservation(@PathVariable Long reservationId) {
        reservationService.releaseReservation(reservationId);
        com.project.bookingservice.dto.reservation.ReleaseReservationResponse data = new com.project.bookingservice.dto.reservation.ReleaseReservationResponse(reservationId, "RELEASED");
        return ResponseEntity.ok(ApiResponse.success("Seat reservation released successfully", data));
    }
}
