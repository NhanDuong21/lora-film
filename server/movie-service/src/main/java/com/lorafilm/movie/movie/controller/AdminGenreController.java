package com.lorafilm.movie.movie.controller;

import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.movie.dto.GenreRequest;
import com.lorafilm.movie.movie.dto.GenreResponse;
import com.lorafilm.movie.movie.service.AdminGenreService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/genres")
public class AdminGenreController {

    private final AdminGenreService adminGenreService;

    public AdminGenreController(AdminGenreService adminGenreService) {
        this.adminGenreService = adminGenreService;
    }

    @GetMapping
    public ApiResponse<com.lorafilm.movie.common.api.PageResponse<GenreResponse>> getGenres(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(adminGenreService.getGenres(page, size));
    }

    @GetMapping("/{publicId}")
    public ApiResponse<GenreResponse> getGenre(@PathVariable String publicId) {
        return ApiResponse.ok(adminGenreService.getGenre(publicId));
    }

    @PostMapping
    public ApiResponse<GenreResponse> createGenre(@Valid @RequestBody GenreRequest request) {
        return ApiResponse.ok(adminGenreService.createGenre(request));
    }

    @PutMapping("/{publicId}")
    public ApiResponse<GenreResponse> updateGenre(
            @PathVariable String publicId, 
            @Valid @RequestBody GenreRequest request) {
        return ApiResponse.ok(adminGenreService.updateGenre(publicId, request));
    }

    @DeleteMapping("/{publicId}")
    public ApiResponse<Void> deleteGenre(@PathVariable String publicId) {
        adminGenreService.deleteGenre(publicId);
        return ApiResponse.ok(null);
    }
}
