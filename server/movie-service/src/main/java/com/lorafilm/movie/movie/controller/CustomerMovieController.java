package com.lorafilm.movie.movie.controller;

import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.common.api.PageResponse;
import com.lorafilm.movie.movie.dto.MovieDto;
import com.lorafilm.movie.movie.service.CustomerMovieService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/movies")
public class CustomerMovieController {

    private final CustomerMovieService customerMovieService;

    public CustomerMovieController(CustomerMovieService customerMovieService) {
        this.customerMovieService = customerMovieService;
    }

    @GetMapping
    public ApiResponse<PageResponse<MovieDto>> getMovies(
            @RequestParam String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "releaseDate"));
        return ApiResponse.ok(customerMovieService.getMoviesByStatus(status, keyword, pageable));
    }

    @GetMapping("/{identifier}")
    public ApiResponse<com.lorafilm.movie.movie.dto.MovieDetailDto> getMovieDetail(@PathVariable String identifier) {
        return ApiResponse.ok(customerMovieService.getMovieDetail(identifier));
    }

    @GetMapping("/{identifier}/credits")
    public ApiResponse<Object> getMovieCredits(@PathVariable String identifier) {
        com.lorafilm.movie.movie.dto.MovieDetailDto detail = customerMovieService.getMovieDetail(identifier);
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("directors", detail.getDirectors());
        response.put("actors", detail.getActors());
        response.put("writers", detail.getWriters());
        response.put("producers", detail.getProducers());
        return ApiResponse.ok(response);
    }

    @GetMapping("/{identifier}/production-companies")
    public ApiResponse<Object> getMovieProductionCompanies(@PathVariable String identifier) {
        com.lorafilm.movie.movie.dto.MovieDetailDto detail = customerMovieService.getMovieDetail(identifier);
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("productionCompanies", detail.getProductionCompanies());
        response.put("distributors", detail.getDistributors());
        response.put("studios", detail.getStudios());
        return ApiResponse.ok(response);
    }
}
