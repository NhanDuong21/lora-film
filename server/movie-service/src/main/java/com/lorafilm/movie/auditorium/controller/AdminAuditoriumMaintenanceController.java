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

@RestController
@RequestMapping("/api/admin")
@ApiResponses(value = {
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict")
})
public class AdminAuditoriumMaintenanceController {

    private final AuditoriumMaintenanceService maintenanceService;

    public AdminAuditoriumMaintenanceController(AuditoriumMaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @PostMapping("/auditoriums/{auditoriumPublicId}/maintenance-windows")
    public ResponseEntity<ApiResponse<MaintenanceWindowResponse>> createWindow(
            @PathVariable String auditoriumPublicId,
            @Valid @RequestBody CreateMaintenanceWindowRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(maintenanceService.createWindow(auditoriumPublicId, request)));
    }

    @PutMapping("/maintenance-windows/{maintenanceWindowId}/cancel")
    public ResponseEntity<ApiResponse<MaintenanceWindowResponse>> cancelWindow(
            @PathVariable Long maintenanceWindowId) {
        return ResponseEntity.ok(ApiResponse.ok(maintenanceService.cancelWindow(maintenanceWindowId)));
    }
}
