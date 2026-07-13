package com.lorafilm.movie.cinema.controller;

import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.cinema.dto.CreateCinemaRequest;
import com.lorafilm.movie.cinema.dto.UpdateCinemaRequest;
import com.lorafilm.movie.cinema.dto.UpdateCinemaStatusRequest;
import com.lorafilm.movie.cinema.dto.CinemaResponse;
import com.lorafilm.movie.cinema.service.CinemaService;
import com.lorafilm.movie.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/cinemas")
public class AdminCinemaController {

    private final CinemaService cinemaService;

    public AdminCinemaController(CinemaService cinemaService) {
        this.cinemaService = cinemaService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CinemaResponse>> createCinema(
            @Valid @RequestBody CreateCinemaRequest request) {
        CinemaResponse response = cinemaService.createCinema(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PutMapping("/{cinemaPublicId}")
    public ResponseEntity<ApiResponse<CinemaResponse>> updateCinema(
            @PathVariable String cinemaPublicId,
            @Valid @RequestBody UpdateCinemaRequest request) {
        CinemaResponse response = cinemaService.updateCinema(cinemaPublicId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{cinemaPublicId}/status")
    public ResponseEntity<ApiResponse<CinemaResponse>> updateCinemaStatus(
            @PathVariable String cinemaPublicId,
            @Valid @RequestBody UpdateCinemaStatusRequest request) {
        CinemaResponse response = cinemaService.updateCinemaStatus(cinemaPublicId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
