package com.lorafilm.movie.cinema.controller;

import com.lorafilm.movie.cinema.dto.CinemaDto;
import com.lorafilm.movie.cinema.dto.CinemaDetailDto;
import com.lorafilm.movie.cinema.dto.CinemaClosurePeriodResponse;
import com.lorafilm.movie.cinema.service.CinemaService;
import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.common.dto.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/cinemas")
public class CinemaController {

    private final CinemaService cinemaService;

    public CinemaController(CinemaService cinemaService) {
        this.cinemaService = cinemaService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CinemaDto>>> getCinemas(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(cinemaService.getCinemas(city, district, keyword, page, size)));
    }

    @GetMapping("/{cinemaIdOrSlug}")
    public ResponseEntity<ApiResponse<CinemaDetailDto>> getCinemaDetail(@PathVariable String cinemaIdOrSlug) {
        return ResponseEntity.ok(ApiResponse.ok(cinemaService.getCinemaByIdentifier(cinemaIdOrSlug)));
    }

    @GetMapping("/{cinemaPublicId}/closure-periods")
    public ResponseEntity<ApiResponse<List<CinemaClosurePeriodResponse>>> getCinemaClosurePeriods(
            @PathVariable String cinemaPublicId) {
        return ResponseEntity.ok(ApiResponse.ok(cinemaService.getCinemaClosurePeriods(cinemaPublicId)));
    }
}
