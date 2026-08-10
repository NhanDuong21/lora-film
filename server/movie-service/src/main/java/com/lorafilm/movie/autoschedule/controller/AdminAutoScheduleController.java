package com.lorafilm.movie.autoschedule.controller;

import com.lorafilm.movie.autoschedule.dto.request.AutoSchedulePreflightRequest;
import com.lorafilm.movie.autoschedule.dto.response.AutoSchedulePreflightResponse;
import com.lorafilm.movie.autoschedule.service.AutoSchedulePreflightService;
import com.lorafilm.movie.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auto-schedules")
@Tag(name = "Admin Demand-Aware Auto Schedule API")
public class AdminAutoScheduleController {

    private final AutoSchedulePreflightService preflightService;

    public AdminAutoScheduleController(AutoSchedulePreflightService preflightService) {
        this.preflightService = preflightService;
    }

    @PostMapping("/preflight")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Validate authoritative facts before generating any schedule candidate")
    public ResponseEntity<ApiResponse<AutoSchedulePreflightResponse>> preflight(
            @Valid @RequestBody AutoSchedulePreflightRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Auto schedule preflight completed successfully",
                preflightService.preflight(request)));
    }
}
