package com.lorafilm.movie.integration.tmdb.controller;

import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.integration.tmdb.dto.TmdbMovieSuggestionDto;
import com.lorafilm.movie.integration.tmdb.service.TmdbMovieSearchService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tmdb/movies")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class TmdbMovieSearchController {

    private final TmdbMovieSearchService searchService;

    public TmdbMovieSearchController(TmdbMovieSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public ApiResponse<List<TmdbMovieSuggestionDto>> search(
            @RequestParam("query") String query,
            @RequestParam(value = "limit", defaultValue = "8") int limit) {
        return ApiResponse.ok(searchService.search(query, limit));
    }
}
