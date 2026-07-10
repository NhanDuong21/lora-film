package com.lorafilm.movie.movie.controller;

import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.common.dto.PageResponse;
import com.lorafilm.movie.movie.dto.MovieDto;
import com.lorafilm.movie.movie.service.MovieService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<MovieDto>>> getMovies(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "releaseDate,desc") String sort) {
        return ResponseEntity.ok(ApiResponse.ok(movieService.getMovies(status, keyword, page, size, sort)));
    }

    @GetMapping("/{movieSlug}")
    public ResponseEntity<ApiResponse<MovieDto>> getMovieDetail(@PathVariable String movieSlug) {
        return ResponseEntity.ok(ApiResponse.ok(movieService.getMovieBySlug(movieSlug)));
    }
}
