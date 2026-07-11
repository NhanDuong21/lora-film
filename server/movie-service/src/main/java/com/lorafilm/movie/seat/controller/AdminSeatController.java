package com.lorafilm.movie.seat.controller;

import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.seat.dto.BulkCreateSeatsRequest;
import com.lorafilm.movie.seat.dto.SeatResponse;
import com.lorafilm.movie.seat.dto.UpdateSeatRequest;

import com.lorafilm.movie.seat.service.SeatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminSeatController {

    private final SeatService seatService;

    public AdminSeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    @Operation(summary = "Bulk create or update seats")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(oneOf = {com.lorafilm.movie.common.api.error.ValidationErrorResponse.class, com.lorafilm.movie.common.api.error.InvalidEnumErrorResponse.class, com.lorafilm.movie.common.api.error.BulkSeatValidationErrorResponse.class}))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.lorafilm.movie.common.api.error.NotFoundErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.lorafilm.movie.common.api.error.ConflictErrorResponse.class)))
    })
    @PostMapping("/auditoriums/{auditoriumPublicId}/seats/bulk")
    public ResponseEntity<ApiResponse<List<SeatResponse>>> bulkCreateSeats(
            @PathVariable String auditoriumPublicId,
            @Valid @RequestBody BulkCreateSeatsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(seatService.bulkCreateSeats(auditoriumPublicId, request)));
    }

    @Operation(summary = "Update a single seat")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(oneOf = {com.lorafilm.movie.common.api.error.ValidationErrorResponse.class, com.lorafilm.movie.common.api.error.InvalidEnumErrorResponse.class}))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.lorafilm.movie.common.api.error.NotFoundErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(oneOf = {com.lorafilm.movie.common.api.error.ConflictErrorResponse.class, com.lorafilm.movie.common.api.error.BusinessErrorResponse.class})))
    })
    @PutMapping("/seats/{seatPublicId}")
    public ResponseEntity<ApiResponse<SeatResponse>> updateSeat(
            @PathVariable String seatPublicId,
            @Valid @RequestBody UpdateSeatRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(seatService.updateSeat(seatPublicId, request)));
    }
}
