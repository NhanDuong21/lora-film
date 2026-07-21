package com.lorafilm.movie.auditorium.controller;

import com.lorafilm.movie.auditorium.dto.CreateMaintenanceWindowRequest;
import com.lorafilm.movie.auditorium.dto.MaintenanceWindowResponse;
import com.lorafilm.movie.auditorium.service.AuditoriumMaintenanceService;
import com.lorafilm.movie.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminAuditoriumMaintenanceController {

    private final AuditoriumMaintenanceService maintenanceService;

    public AdminAuditoriumMaintenanceController(AuditoriumMaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @Operation(summary = "Create a maintenance window")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(oneOf = {com.lorafilm.movie.common.api.error.ValidationErrorResponse.class, com.lorafilm.movie.common.api.error.InvalidDateFormatErrorResponse.class}))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.lorafilm.movie.common.api.error.NotFoundErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.lorafilm.movie.common.api.error.BusinessErrorResponse.class)))
    })
    @PostMapping("/auditoriums/{auditoriumPublicId}/maintenance-windows")
    public ResponseEntity<ApiResponse<MaintenanceWindowResponse>> createWindow(
            @PathVariable String auditoriumPublicId,
            @Valid @RequestBody CreateMaintenanceWindowRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(maintenanceService.createWindow(auditoriumPublicId, request)));
    }
    
    @Operation(summary = "Get all maintenance windows for an auditorium")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Auditorium Not Found", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.lorafilm.movie.common.api.error.NotFoundErrorResponse.class)))
    })
    @GetMapping("/auditoriums/{auditoriumPublicId}/maintenance-windows")
    public ResponseEntity<ApiResponse<List<MaintenanceWindowResponse>>> getMaintenanceWindows(
            @PathVariable String auditoriumPublicId) {
        return ResponseEntity.ok(ApiResponse.ok(maintenanceService.getMaintenanceWindows(auditoriumPublicId)));
    }

    @Operation(summary = "Cancel a maintenance window")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.lorafilm.movie.common.api.error.NotFoundErrorResponse.class)))
    })
    @PutMapping("/maintenance-windows/{maintenanceWindowId}/cancel")
    public ResponseEntity<ApiResponse<MaintenanceWindowResponse>> cancelWindow(
            @PathVariable Long maintenanceWindowId) {
        return ResponseEntity.ok(ApiResponse.ok(maintenanceService.cancelWindow(maintenanceWindowId)));
    }
}
