package com.project.movieservice.controller;

import com.project.movieservice.common.ApiResponse;
import com.project.movieservice.dto.GenreCreateRequest;
import com.project.movieservice.dto.GenreResponse;
import com.project.movieservice.dto.GenreUpdateRequest;
import com.project.movieservice.service.GenreService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/genres")
public class AdminGenreController {

    private final GenreService genreService;

    public AdminGenreController(GenreService genreService) {
        this.genreService = genreService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<GenreResponse>> createGenre(@Valid @RequestBody GenreCreateRequest request) {
        GenreResponse data = genreService.createGenre(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Genre created successfully", data));
    }

    @PutMapping("/{genreId}")
    public ResponseEntity<ApiResponse<GenreResponse>> updateGenre(
            @PathVariable Integer genreId,
            @Valid @RequestBody GenreUpdateRequest request) {
        GenreResponse data = genreService.updateGenre(genreId, request);
        return ResponseEntity.ok(ApiResponse.success("Genre updated successfully", data));
    }

    @DeleteMapping("/{genreId}")
    public ResponseEntity<ApiResponse<Void>> deleteGenre(@PathVariable Integer genreId) {
        genreService.softDeleteGenre(genreId);
        return ResponseEntity.ok(ApiResponse.success("Genre deleted successfully", null));
    }
}
