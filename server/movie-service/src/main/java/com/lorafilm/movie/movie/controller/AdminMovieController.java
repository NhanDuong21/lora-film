package com.lorafilm.movie.movie.controller;

import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.movie.dto.MovieDto;
import com.lorafilm.movie.movie.dto.MovieGenreAssignRequest;
import com.lorafilm.movie.movie.dto.MovieRequest;
import com.lorafilm.movie.movie.service.AdminMovieService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/movies")
public class AdminMovieController {

    private final AdminMovieService adminMovieService;

    public AdminMovieController(AdminMovieService adminMovieService) {
        this.adminMovieService = adminMovieService;
    }

    @PostMapping
    public ApiResponse<MovieDto> createMovie(@Valid @RequestBody MovieRequest request) {
        return ApiResponse.ok(adminMovieService.createMovie(request));
    }

    @PutMapping("/{publicId}")
    public ApiResponse<MovieDto> updateMovie(
            @PathVariable String publicId, 
            @Valid @RequestBody MovieRequest request) {
        return ApiResponse.ok(adminMovieService.updateMovie(publicId, request));
    }

    @PostMapping("/{publicId}/genres")
    public ApiResponse<String> assignGenresPost(
            @PathVariable String publicId,
            @Valid @RequestBody MovieGenreAssignRequest request) {
        adminMovieService.assignGenres(publicId, request.getGenreIds());
        return ApiResponse.ok("Genres assigned successfully");
    }

    @PutMapping("/{publicId}/genres")
    public ApiResponse<String> assignGenresPut(
            @PathVariable String publicId,
            @Valid @RequestBody MovieGenreAssignRequest request) {
        adminMovieService.assignGenres(publicId, request.getGenreIds());
        return ApiResponse.ok("Genres updated successfully");
    }

    @DeleteMapping("/{publicId}")
    public ApiResponse<String> deleteMovie(@PathVariable String publicId) {
        adminMovieService.deleteMovie(publicId);
        return ApiResponse.ok("Movie deleted successfully");
    }
}
