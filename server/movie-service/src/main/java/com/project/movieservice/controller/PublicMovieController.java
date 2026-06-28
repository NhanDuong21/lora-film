package com.project.movieservice.controller;

import com.project.movieservice.common.ApiResponse;
import com.project.movieservice.dto.MovieDetailResponse;
import com.project.movieservice.dto.MovieListItemResponse;
import com.project.movieservice.dto.MoviePageResponse;
import com.project.movieservice.service.MovieService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/movies")
public class PublicMovieController {

    private final MovieService movieService;

    public PublicMovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<MoviePageResponse<MovieListItemResponse>>> getMovies(
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String genreId,
            @RequestParam(required = false) String releaseFrom,
            @RequestParam(required = false) String releaseTo,
            @RequestParam(required = false) String sort) {

        MoviePageResponse<MovieListItemResponse> movies = movieService.getMovies(
                page, size, search, status, genreId, releaseFrom, releaseTo, sort);
        return ResponseEntity.ok(ApiResponse.success("Movies retrieved successfully", movies));
    }

    @GetMapping("/{movieId}")
    public ResponseEntity<ApiResponse<MovieDetailResponse>> getMovieDetail(@PathVariable String movieId) {
        MovieDetailResponse movie = movieService.getMovieDetail(movieId);
        return ResponseEntity.ok(ApiResponse.success("Movie retrieved successfully", movie));
    }
}
