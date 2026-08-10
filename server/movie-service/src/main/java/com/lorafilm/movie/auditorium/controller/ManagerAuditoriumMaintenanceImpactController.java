package com.lorafilm.movie.auditorium.controller;

import com.lorafilm.movie.auditorium.dto.CreateMaintenanceWindowRequest;
import com.lorafilm.movie.auditorium.dto.MaintenanceImpactResponse;
import com.lorafilm.movie.auditorium.service.AuditoriumMaintenanceImpactService;
import com.lorafilm.movie.cinema.dto.CinemaDetailDto;
import com.lorafilm.movie.cinema.service.CinemaService;
import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.security.ManagerCinemaScopeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager/cinemas/{cinemaPublicId}/auditoriums")
@PreAuthorize("hasAuthority('ROLE_MANAGER')")
public class ManagerAuditoriumMaintenanceImpactController {

    private final AuditoriumMaintenanceImpactService impactService;
    private final ManagerCinemaScopeService cinemaScope;
    private final CinemaService cinemaService;

    public ManagerAuditoriumMaintenanceImpactController(
            AuditoriumMaintenanceImpactService impactService,
            ManagerCinemaScopeService cinemaScope,
            CinemaService cinemaService) {
        this.impactService = impactService;
        this.cinemaScope = cinemaScope;
        this.cinemaService = cinemaService;
    }

    @PostMapping("/{auditoriumPublicId}/maintenance-windows/impact-preview")
    public ResponseEntity<ApiResponse<MaintenanceImpactResponse>> preview(
            @PathVariable String cinemaPublicId,
            @PathVariable String auditoriumPublicId,
            @Valid @RequestBody CreateMaintenanceWindowRequest request) {
        cinemaScope.requireAssigned(cinemaPublicId);
        CinemaDetailDto cinema = cinemaService.getAdminCinemaDetail(cinemaPublicId);
        boolean belongsToCinema = cinema.getActiveAuditoriums().stream()
                .anyMatch(room -> auditoriumPublicId.equalsIgnoreCase(room.getPublicId()));
        if (!belongsToCinema) {
            throw new BusinessException(
                    ErrorCode.ACCESS_DENIED,
                    "Phòng chiếu không thuộc rạp được phân công.");
        }
        return ResponseEntity.ok(ApiResponse.ok(
                impactService.preview(auditoriumPublicId, request)));
    }
}
