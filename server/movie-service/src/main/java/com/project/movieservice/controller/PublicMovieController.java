package com.project.movieservice.controller;

import com.project.movieservice.common.ApiResponse;
import com.project.movieservice.dto.MoviePageResponse;
import com.project.movieservice.service.MovieService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/movies")
public class PublicMovieController {

    private final MovieService movieService;

    public PublicMovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    private boolean hasAdminRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") 
                            || a.getAuthority().equals("ROLE_MOVIE_MANAGE")
                            || a.getAuthority().equals("ROLE_MOVIE_READ"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<MoviePageResponse<?>>> getMovies(
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String genreId,
            @RequestParam(required = false) String releaseFrom,
            @RequestParam(required = false) String releaseTo,
            @RequestParam(required = false) String sort) {

        boolean isAdmin = hasAdminRole();
        MoviePageResponse<?> movies = movieService.getMovies(
                page, size, search, status, genreId, releaseFrom, releaseTo, sort, isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Movies retrieved successfully", movies));
    }

    @GetMapping("/{movieId}")
    public ResponseEntity<ApiResponse<Object>> getMovieDetail(@PathVariable String movieId) {
        boolean isAdmin = hasAdminRole();
        Object movie = movieService.getMovieDetail(movieId, isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Movie retrieved successfully", movie));
    }
}
