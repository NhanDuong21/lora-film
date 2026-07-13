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
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page - 1, size);
        return ApiResponse.ok(customerMovieService.getMoviesByStatus(status, pageable));
    }

    @GetMapping("/{identifier}")
    public ApiResponse<MovieDto> getMovieDetail(@PathVariable String identifier) {
        return ApiResponse.ok(customerMovieService.getMovieDetail(identifier));
    }
}
