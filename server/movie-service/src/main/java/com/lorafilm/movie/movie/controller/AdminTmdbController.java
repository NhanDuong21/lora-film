package com.lorafilm.movie.movie.controller;

import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.movie.dto.MovieDto;
import com.lorafilm.movie.movie.dto.tmdb.TmdbApproveRequest;
import com.lorafilm.movie.movie.service.TmdbService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/tmdb")
public class AdminTmdbController {

    private final TmdbService tmdbService;

    public AdminTmdbController(TmdbService tmdbService) {
        this.tmdbService = tmdbService;
    }

    @PostMapping("/approve")
    @Deprecated(forRemoval = true)
    public ApiResponse<MovieDto> approveTmdbMovie(@Valid @RequestBody TmdbApproveRequest request) {
        return ApiResponse.ok(tmdbService.approveTmdbMovie(request.getTmdbId()));
    }
}
