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
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Seats reserved successfully",
            content = @Content(mediaType = "application/json",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{ \"success\": true, \"message\": \"Success\", \"errorCode\": null, \"data\": { \"showtimeId\": 123, \"userId\": 1, \"status\": \"HELD\", \"expiresAt\": \"2026-06-28T10:00:00\", \"seats\": [{ \"id\": 1, \"seatId\": 10 }] }, \"errors\": null }")))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{ \"success\": false, \"message\": \"Validation failed\", \"errorCode\": \"VALIDATION_ERROR\", \"data\": null, \"errors\": [ { \"field\": \"seatIds\", \"message\": \"seatIds cannot be empty\" } ] }")))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{ \"success\": false, \"message\": \"Unauthorized\", \"errorCode\": \"UNAUTHORIZED\", \"data\": null, \"errors\": null }")))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{ \"success\": false, \"message\": \"Showtime not found\", \"errorCode\": \"BOOKING_SHOWTIME_NOT_FOUND\", \"data\": null, \"errors\": null }")))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{ \"success\": false, \"message\": \"One or more seats are already reserved\", \"errorCode\": \"SEAT_ALREADY_RESERVED\", \"data\": null, \"errors\": null }")))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "Service Unavailable",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{ \"success\": false, \"message\": \"Seat reservation service is temporarily unavailable\", \"errorCode\": \"SEAT_LOCK_SERVICE_UNAVAILABLE\", \"data\": null, \"errors\": null }")))
    ResponseEntity<ApiResponse<ReservationGroupResponse>> createReservation(String idempotencyKey, CreateReservationRequest request);

    @Operation(summary = "Get a seat reservation", description = "Retrieve details of a specific seat reservation by ID. User must own the reservation.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seat reservation retrieved successfully",
            content = @Content(mediaType = "application/json",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{ \"success\": true, \"message\": \"Success\", \"errorCode\": null, \"data\": { \"id\": 1, \"userId\": 1, \"showtimeId\": 123, \"seatId\": 10, \"status\": \"HELD\", \"expiresAt\": \"2026-06-28T10:00:00\", \"createdAt\": \"2026-06-28T09:45:00\" }, \"errors\": null }")))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{ \"success\": false, \"message\": \"Unauthorized\", \"errorCode\": \"UNAUTHORIZED\", \"data\": null, \"errors\": null }")))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{ \"success\": false, \"message\": \"You cannot access this reservation\", \"errorCode\": \"FORBIDDEN\", \"data\": null, \"errors\": null }")))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{ \"success\": false, \"message\": \"Seat reservation not found\", \"errorCode\": \"SEAT_RESERVATION_NOT_FOUND\", \"data\": null, \"errors\": null }")))
    ResponseEntity<ApiResponse<ReservationResponse>> getReservation(Long reservationId);

    @Operation(summary = "Release a seat reservation", description = "Release a held seat reservation. Idempotent operation. User must own the reservation.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seat reservation released successfully",
            content = @Content(mediaType = "application/json",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{ \"success\": true, \"message\": \"Success\", \"errorCode\": null, \"data\": { \"reservationId\": 1, \"status\": \"RELEASED\" }, \"errors\": null }")))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{ \"success\": false, \"message\": \"Unauthorized\", \"errorCode\": \"UNAUTHORIZED\", \"data\": null, \"errors\": null }")))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{ \"success\": false, \"message\": \"You cannot access this reservation\", \"errorCode\": \"FORBIDDEN\", \"data\": null, \"errors\": null }")))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{ \"success\": false, \"message\": \"Reservation not found\", \"errorCode\": \"RESERVATION_NOT_FOUND\", \"data\": null, \"errors\": null }")))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{ \"success\": false, \"message\": \"Seat reservation has already been converted to a booking\", \"errorCode\": \"SEAT_RESERVATION_ALREADY_CONVERTED\", \"data\": null, \"errors\": null }")))
    ResponseEntity<ApiResponse<ReleaseReservationResponse>> releaseReservation(Long reservationId);
}
