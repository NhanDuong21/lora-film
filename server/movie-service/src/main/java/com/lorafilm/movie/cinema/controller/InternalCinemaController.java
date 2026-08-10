package com.lorafilm.movie.cinema.controller;

import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.common.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/cinemas")
public class InternalCinemaController {

    private final CinemaRepository cinemaRepository;

    public InternalCinemaController(CinemaRepository cinemaRepository) {
        this.cinemaRepository = cinemaRepository;
    }

    @GetMapping("/{publicId}/exists")
    public ResponseEntity<ApiResponse<CinemaExistenceResponse>> exists(@PathVariable String publicId) {
        boolean exists = cinemaRepository.existsByPublicIdAndDeletedAtIsNull(
                publicId == null ? "" : publicId.trim().toLowerCase(java.util.Locale.ROOT));
        return ResponseEntity.ok(ApiResponse.ok(new CinemaExistenceResponse(exists)));
    }

    public record CinemaExistenceResponse(boolean exists) {
    }
}
