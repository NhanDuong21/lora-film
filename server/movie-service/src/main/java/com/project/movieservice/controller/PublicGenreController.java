package com.project.movieservice.controller;

import com.project.movieservice.common.ApiResponse;
import com.project.movieservice.dto.GenreResponse;
import com.project.movieservice.service.GenreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/genres")
public class PublicGenreController {

    private final GenreService genreService;

    public PublicGenreController(GenreService genreService) {
        this.genreService = genreService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<GenreResponse>>> getGenres() {
        List<GenreResponse> data = genreService.getGenres();
        return ResponseEntity.ok(ApiResponse.success("Genres retrieved successfully", data));
    }

    @GetMapping("/{genreId}")
    public ResponseEntity<ApiResponse<GenreResponse>> getGenreById(@PathVariable Integer genreId) {
        GenreResponse data = genreService.getGenreById(genreId);
        return ResponseEntity.ok(ApiResponse.success("Genre retrieved successfully", data));
    }
}
