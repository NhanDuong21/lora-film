package com.lorafilm.movie.seat.controller;

import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.seat.dto.CreateSeatTypeRequest;
import com.lorafilm.movie.seat.dto.SeatTypeResponse;
import com.lorafilm.movie.seat.dto.UpdateSeatTypeRequest;

import com.lorafilm.movie.seat.service.SeatTypeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/seat-types")
public class AdminSeatTypeController {

    private final SeatTypeService seatTypeService;

    public AdminSeatTypeController(SeatTypeService seatTypeService) {
        this.seatTypeService = seatTypeService;
    }

    @Operation(summary = "Create a seat type")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(oneOf = {com.lorafilm.movie.common.api.error.ValidationErrorResponse.class, com.lorafilm.movie.common.api.error.InvalidEnumErrorResponse.class}))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.lorafilm.movie.common.api.error.ConflictErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ApiResponse<SeatTypeResponse>> createSeatType(
            @Valid @RequestBody CreateSeatTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(seatTypeService.createSeatType(request)));
    }

    @Operation(summary = "Update a seat type")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(oneOf = {com.lorafilm.movie.common.api.error.ValidationErrorResponse.class, com.lorafilm.movie.common.api.error.InvalidEnumErrorResponse.class}))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.lorafilm.movie.common.api.error.NotFoundErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.lorafilm.movie.common.api.error.ConflictErrorResponse.class)))
    })
    @PutMapping("/{seatTypePublicId}")
    public ResponseEntity<ApiResponse<SeatTypeResponse>> updateSeatType(
            @PathVariable String seatTypePublicId,
            @Valid @RequestBody UpdateSeatTypeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(seatTypeService.updateSeatType(seatTypePublicId, request)));
    }

    @Operation(summary = "Get all seat types")
    @GetMapping
    public ResponseEntity<ApiResponse<java.util.List<SeatTypeResponse>>> getAllSeatTypes() {
        return ResponseEntity.ok(ApiResponse.ok(seatTypeService.getAllSeatTypes()));
    }
}
