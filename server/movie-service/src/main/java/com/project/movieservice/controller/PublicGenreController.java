package com.project.movieservice.controller;

import com.project.movieservice.common.ApiResponse;
import com.project.movieservice.dto.GenreResponse;
import com.project.movieservice.service.GenreService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/genres")
public class PublicGenreController {

    private final GenreService genreService;

    public PublicGenreController(GenreService genreService) {
        this.genreService = genreService;
    }

    private boolean hasAdminRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") 
                            || a.getAuthority().equals("ROLE_GENRE_MANAGE"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<GenreResponse>>> getGenres() {
        boolean isAdmin = hasAdminRole();
        List<GenreResponse> data = genreService.getGenres(isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Genres retrieved successfully", data));
    }

    @GetMapping("/{genreId}")
    public ResponseEntity<ApiResponse<GenreResponse>> getGenreById(@PathVariable Integer genreId) {
        boolean isAdmin = hasAdminRole();
        GenreResponse data = genreService.getGenreById(genreId, isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Genre retrieved successfully", data));
    }
}
