package com.lorafilm.movie.movie.controller;

import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.common.dto.PageResponse;
import com.lorafilm.movie.movie.dto.MovieDto;
import com.lorafilm.movie.movie.dto.MovieDetailDto;
import com.lorafilm.movie.movie.service.MovieService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

@RestController
@Validated
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping("/api/movies")
    public ResponseEntity<ApiResponse<PageResponse<MovieDto>>> getMovies(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @Min(1) Long genreId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) @Min(1) Long cinemaId,
            @RequestParam(required = false) java.time.LocalDate date,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "releaseDate,desc") String sort) {
        return ResponseEntity.ok(ApiResponse.ok(movieService.getMovies(status, genreId, keyword, city, cinemaId, date, page, size, sort)));
    }

    @GetMapping("/api/movies/{movieId}")
    public ResponseEntity<ApiResponse<MovieDetailDto>> getMovieDetail(@PathVariable("movieId") String movieId) {
        return ResponseEntity.ok(ApiResponse.ok(movieService.getMovieByIdentifier(movieId)));
    }

    @PutMapping("/api/admin/movies/{movieId}/status")
    public ResponseEntity<ApiResponse<MovieDto>> updateMovieStatus(
            @PathVariable("movieId") String movieId,
            @RequestParam("status") MovieStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(movieService.updateMovieStatus(movieId, status)));
    }
}
