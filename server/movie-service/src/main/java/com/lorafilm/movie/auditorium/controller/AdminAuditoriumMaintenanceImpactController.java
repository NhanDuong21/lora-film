package com.lorafilm.movie.auditorium.controller;

import com.lorafilm.movie.auditorium.dto.CreateMaintenanceWindowRequest;
import com.lorafilm.movie.auditorium.dto.MaintenanceImpactResponse;
import com.lorafilm.movie.auditorium.service.AuditoriumMaintenanceImpactService;
import com.lorafilm.movie.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auditoriums")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminAuditoriumMaintenanceImpactController {

    private final AuditoriumMaintenanceImpactService impactService;

    public AdminAuditoriumMaintenanceImpactController(
            AuditoriumMaintenanceImpactService impactService) {
        this.impactService = impactService;
    }

    @PostMapping("/{auditoriumPublicId}/maintenance-windows/impact-preview")
    public ResponseEntity<ApiResponse<MaintenanceImpactResponse>> preview(
            @PathVariable String auditoriumPublicId,
            @Valid @RequestBody CreateMaintenanceWindowRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                impactService.preview(auditoriumPublicId, request)));
    }
}
