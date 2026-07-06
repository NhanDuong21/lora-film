package com.project.movieservice.controller;

import com.project.movieservice.common.ApiResponse;
import com.project.movieservice.dto.*;
import com.project.movieservice.service.MovieService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/movies")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMovieController {

    private final MovieService movieService;

    public AdminMovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MovieCreatedResponse>> createMovie(@Valid @RequestBody MovieCreateRequest request) {
        MovieCreatedResponse response = movieService.createMovie(request);
        return new ResponseEntity<>(ApiResponse.success("Movie created successfully", response), HttpStatus.CREATED);
    }

    @PutMapping("/{movieId}")
    public ResponseEntity<ApiResponse<MovieUpdatedResponse>> updateMovie(
            @PathVariable String movieId,
            @Valid @RequestBody MovieUpdateRequest request) {
        MovieUpdatedResponse response = movieService.updateMovie(movieId, request);
        return ResponseEntity.ok(ApiResponse.success("Movie updated successfully", response));
    }

    @DeleteMapping("/{movieId}")
    public ResponseEntity<ApiResponse<Void>> deleteMovie(@PathVariable String movieId) {
        movieService.softDeleteMovie(movieId);
        return ResponseEntity.ok(ApiResponse.success("Movie deleted successfully", null));
    }
}
