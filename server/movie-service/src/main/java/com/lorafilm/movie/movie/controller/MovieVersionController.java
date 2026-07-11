package com.lorafilm.movie.movie.controller;

import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.movie.dto.CreateMovieVersionRequest;
import com.lorafilm.movie.movie.dto.MovieVersionResponse;
import com.lorafilm.movie.movie.dto.UpdateMovieVersionRequest;
import com.lorafilm.movie.movie.service.MovieVersionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class MovieVersionController {

    private final MovieVersionService movieVersionService;

    public MovieVersionController(MovieVersionService movieVersionService) {
        this.movieVersionService = movieVersionService;
    }

    // --- Customer APIs ---

    @GetMapping("/api/movies/{movieId}/versions")
    public ApiResponse<List<MovieVersionResponse>> getActiveVersions(@PathVariable("movieId") String movieId) {
        List<MovieVersionResponse> versions = movieVersionService.getActiveVersionsByMovie(movieId);
        return ApiResponse.ok(versions);
    }

    // --- Admin APIs ---

    @PostMapping("/api/admin/movies/{movieId}/versions")
    public ApiResponse<MovieVersionResponse> createVersion(
            @PathVariable("movieId") String movieId,
            @Valid @RequestBody CreateMovieVersionRequest request
    ) {
        MovieVersionResponse response = movieVersionService.createVersion(movieId, request);
        return ApiResponse.ok(response);
    }

    @PutMapping("/api/admin/movie-versions/{versionId}")
    public ApiResponse<MovieVersionResponse> updateVersion(
            @PathVariable("versionId") String versionId,
            @Valid @RequestBody UpdateMovieVersionRequest request
    ) {
        MovieVersionResponse response = movieVersionService.updateVersion(versionId, request);
        return ApiResponse.ok(response);
    }
}
