package com.lorafilm.movie.movie.controller;

import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.common.api.PageResponse;
import com.lorafilm.movie.movie.dto.MovieDto;
import com.lorafilm.movie.movie.service.CustomerMovieService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import com.lorafilm.movie.showtime.dto.response.CustomerBookingOptionResponse;
import com.lorafilm.movie.showtime.service.CustomerShowtimeService;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/customer/movies")
public class CustomerMovieController {

    private final CustomerMovieService customerMovieService;
    private final CustomerShowtimeService customerShowtimeService;

    public CustomerMovieController(CustomerMovieService customerMovieService,
                                   CustomerShowtimeService customerShowtimeService) {
        this.customerMovieService = customerMovieService;
        this.customerShowtimeService = customerShowtimeService;
    }

    @GetMapping("/{identifier}/booking-options")
    public ApiResponse<List<CustomerBookingOptionResponse>> getBookingOptions(
            @PathVariable String identifier,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        customerMovieService.getMovieDetail(identifier);
        return ApiResponse.ok(customerShowtimeService.getBookingOptions(identifier, from, to));
    }

    @GetMapping
    public ApiResponse<PageResponse<MovieDto>> getMovies(
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String genrePublicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "releaseDate,desc") String sort) {
        String[] sortParts = sort.split(",", 2);
        String property = switch (sortParts[0]) {
            case "createdAt" -> "createdAt";
            case "releaseDate" -> "releaseDate";
            case "title" -> "title";
            default -> throw new com.lorafilm.movie.common.exception.BusinessException(
                    com.lorafilm.movie.common.exception.ErrorCode.VALIDATION_ERROR,
                    "Chỉ có thể sắp xếp phim theo ngày tạo, ngày khai thác hoặc tên phim.");
        };
        org.springframework.data.domain.Sort.Direction direction =
                sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1])
                        ? org.springframework.data.domain.Sort.Direction.ASC
                        : org.springframework.data.domain.Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by(direction, property));
        return ApiResponse.ok(customerMovieService.getMoviesByStatus(
                status,
                keyword,
                genrePublicId,
                pageable));
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
