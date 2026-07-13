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

import com.lorafilm.movie.cinema.dto.CreateCinemaMediaRequest;
import com.lorafilm.movie.cinema.dto.UpdateCinemaMediaRequest;
import com.lorafilm.movie.cinema.dto.CinemaMediaResponse;
import com.lorafilm.movie.cinema.dto.OperatingHourUpdateRequest;
import com.lorafilm.movie.cinema.dto.OperatingHourResponse;
import com.lorafilm.movie.cinema.dto.CreateCinemaClosurePeriodRequest;
import com.lorafilm.movie.cinema.dto.CinemaClosurePeriodResponse;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminCinemaController {

    private final CinemaService cinemaService;

    public AdminCinemaController(CinemaService cinemaService) {
        this.cinemaService = cinemaService;
    }

    @PostMapping("/cinemas")
    public ResponseEntity<ApiResponse<CinemaResponse>> createCinema(
            @Valid @RequestBody CreateCinemaRequest request) {
        CinemaResponse response = cinemaService.createCinema(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PutMapping("/cinemas/{cinemaPublicId}")
    public ResponseEntity<ApiResponse<CinemaResponse>> updateCinema(
            @PathVariable String cinemaPublicId,
            @Valid @RequestBody UpdateCinemaRequest request) {
        CinemaResponse response = cinemaService.updateCinema(cinemaPublicId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/cinemas/{cinemaPublicId}/status")
    public ResponseEntity<ApiResponse<CinemaResponse>> updateCinemaStatus(
            @PathVariable String cinemaPublicId,
            @Valid @RequestBody UpdateCinemaStatusRequest request) {
        CinemaResponse response = cinemaService.updateCinemaStatus(cinemaPublicId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/cinemas/{cinemaPublicId}/media")
    public ResponseEntity<ApiResponse<CinemaMediaResponse>> addCinemaMedia(
            @PathVariable String cinemaPublicId,
            @Valid @RequestBody CreateCinemaMediaRequest request) {
        CinemaMediaResponse response = cinemaService.addCinemaMedia(cinemaPublicId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PutMapping("/cinema-media/{mediaPublicId}")
    public ResponseEntity<ApiResponse<CinemaMediaResponse>> updateCinemaMedia(
            @PathVariable String mediaPublicId,
            @Valid @RequestBody UpdateCinemaMediaRequest request) {
        CinemaMediaResponse response = cinemaService.updateCinemaMedia(mediaPublicId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/cinemas/{cinemaPublicId}/operating-hours")
    public ResponseEntity<ApiResponse<List<OperatingHourResponse>>> updateOperatingHours(
            @PathVariable String cinemaPublicId,
            @Valid @RequestBody List<OperatingHourUpdateRequest> requests) {
        List<OperatingHourResponse> response = cinemaService.updateOperatingHours(cinemaPublicId, requests);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/cinemas/{cinemaPublicId}/closure-periods")
    public ResponseEntity<ApiResponse<CinemaClosurePeriodResponse>> createClosurePeriod(
            @PathVariable String cinemaPublicId,
            @Valid @RequestBody CreateCinemaClosurePeriodRequest request) {
        CinemaClosurePeriodResponse response = cinemaService.createClosurePeriod(cinemaPublicId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PutMapping("/closure-periods/{closurePeriodId}/cancel")
    public ResponseEntity<ApiResponse<CinemaClosurePeriodResponse>> cancelClosurePeriod(
            @PathVariable Long closurePeriodId) {
        CinemaClosurePeriodResponse response = cinemaService.cancelClosurePeriod(closurePeriodId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
