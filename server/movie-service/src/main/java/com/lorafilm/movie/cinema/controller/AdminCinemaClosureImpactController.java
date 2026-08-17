package com.lorafilm.movie.cinema.controller;

import com.lorafilm.movie.cinema.dto.CinemaClosureImpactResponse;
import com.lorafilm.movie.cinema.dto.CreateCinemaClosurePeriodRequest;
import com.lorafilm.movie.cinema.service.CinemaClosureImpactService;
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
@RequestMapping("/api/admin/cinemas")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminCinemaClosureImpactController {

    private final CinemaClosureImpactService impactService;

    public AdminCinemaClosureImpactController(CinemaClosureImpactService impactService) {
        this.impactService = impactService;
    }

    @PostMapping("/{cinemaPublicId}/closure-periods/impact-preview")
    public ResponseEntity<ApiResponse<CinemaClosureImpactResponse>> preview(
            @PathVariable String cinemaPublicId,
            @Valid @RequestBody CreateCinemaClosurePeriodRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(impactService.preview(cinemaPublicId, request)));
    }
}
