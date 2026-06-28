package com.project.bookingservice.api;

import com.project.bookingservice.common.ApiResponse;
import com.project.bookingservice.dto.reservation.CreateReservationRequest;
import com.project.bookingservice.dto.reservation.ReleaseReservationResponse;
import com.project.bookingservice.dto.reservation.ReservationGroupResponse;
import com.project.bookingservice.dto.reservation.ReservationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Seat Reservation API", description = "Endpoints for managing seat reservations")
public interface SeatReservationApi {

    @Operation(summary = "Create a new seat reservation", description = "Reserves one or more seats for a showtime atomically. Requires Idempotency-Key header.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Seats reserved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "Service Unavailable")
    ResponseEntity<ApiResponse<ReservationGroupResponse>> createReservation(String idempotencyKey, CreateReservationRequest request);

    @Operation(summary = "Get a seat reservation", description = "Retrieve details of a specific seat reservation by ID. User must own the reservation.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seat reservation retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found")
    ResponseEntity<ApiResponse<ReservationResponse>> getReservation(Long reservationId);

    @Operation(summary = "Release a seat reservation", description = "Release a held seat reservation. Idempotent operation. User must own the reservation.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seat reservation released successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict")
    ResponseEntity<ApiResponse<ReleaseReservationResponse>> releaseReservation(Long reservationId);
}
