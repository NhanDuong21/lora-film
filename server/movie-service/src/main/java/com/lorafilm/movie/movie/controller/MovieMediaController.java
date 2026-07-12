package com.lorafilm.movie.movie.controller;

import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.movie.dto.CreateMovieMediaRequest;
import com.lorafilm.movie.movie.dto.MovieMediaResponse;
import com.lorafilm.movie.movie.dto.UpdateMovieMediaRequest;
import com.lorafilm.movie.movie.service.MovieMediaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class MovieMediaController {

    private final MovieMediaService movieMediaService;

    public MovieMediaController(MovieMediaService movieMediaService) {
        this.movieMediaService = movieMediaService;
    }

    // --- Customer APIs ---

    @GetMapping("/api/movies/{movieId}/media")
    public ApiResponse<List<MovieMediaResponse>> getCustomerMedia(@PathVariable("movieId") String movieId) {
        List<MovieMediaResponse> media = movieMediaService.getCustomerMedia(movieId);
        return ApiResponse.ok(media);
    }

    // --- Admin APIs ---

    @GetMapping("/api/admin/movies/{movieId}/media")
    public ApiResponse<List<MovieMediaResponse>> getAdminMedia(@PathVariable("movieId") String movieId) {
        List<MovieMediaResponse> media = movieMediaService.getMovieMedia(movieId);
        return ApiResponse.ok(media);
    }

    @PostMapping("/api/admin/movies/{movieId}/media")
    public ApiResponse<MovieMediaResponse> createMedia(
            @PathVariable("movieId") String movieId,
            @Valid @RequestBody CreateMovieMediaRequest request
    ) {
        MovieMediaResponse response = movieMediaService.createMedia(movieId, request);
        return ApiResponse.ok(response);
    }

    @GetMapping("/api/admin/movie-media/{mediaId}")
    public ApiResponse<MovieMediaResponse> getMedia(@PathVariable("mediaId") String mediaId) {
        MovieMediaResponse response = movieMediaService.getMedia(mediaId);
        return ApiResponse.ok(response);
    }

    @PutMapping("/api/admin/movie-media/{mediaId}")
    public ApiResponse<MovieMediaResponse> updateMedia(
            @PathVariable("mediaId") String mediaId,
            @Valid @RequestBody UpdateMovieMediaRequest request
    ) {
        MovieMediaResponse response = movieMediaService.updateMedia(mediaId, request);
        return ApiResponse.ok(response);
    }

    @DeleteMapping("/api/admin/movie-media/{mediaId}")
    public ApiResponse<Void> deleteMedia(@PathVariable("mediaId") String mediaId) {
        movieMediaService.deleteMedia(mediaId);
        return ApiResponse.ok("Movie media deleted successfully");
    }
}
