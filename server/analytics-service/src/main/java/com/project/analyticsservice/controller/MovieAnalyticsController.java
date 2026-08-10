package com.project.analyticsservice.controller;

import com.project.analyticsservice.common.ApiResponse;
import com.project.analyticsservice.dto.*;
import com.project.analyticsservice.application.MovieAnalyticsApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics/movies")
@Tag(name = "Movie Analytics API", description = "Endpoints for Movie Revenue and Statistics Reports")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
public class MovieAnalyticsController {

    private final MovieAnalyticsApplicationService movieAnalyticsService;

    public MovieAnalyticsController(MovieAnalyticsApplicationService movieAnalyticsService) {
        this.movieAnalyticsService = movieAnalyticsService;
    }

    @GetMapping
    @Operation(summary = "Get movie revenue statistics list", description = "Supports paging, searching, filtering, and sorting in either LIFETIME or DATE_RANGE mode.")
    public ApiResponse<MovieRevenueListResponse> getMovieRevenueList(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "movieId", required = false) Long movieId,
            @RequestParam(value = "movieTitle", required = false) String movieTitle,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "direction", required = false) String direction) {

        MovieRevenueListResponse data = movieAnalyticsService.getMovieRevenueList(
                page, size, movieId, movieTitle, startDate, endDate, sortBy, direction);

        return ApiResponse.success("Movie revenue statistics retrieved successfully", data);
    }

    @GetMapping("/top")
    @Operation(summary = "Get top movies by metric", description = "Retrieves top movie ranking based on total revenue or tickets sold.")
    public ApiResponse<TopMoviesResponse> getTopMovies(
            @RequestParam(value = "metric", required = false, defaultValue = "REVENUE") String metric,
            @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
            @RequestParam(value = "direction", required = false, defaultValue = "desc") String direction,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {

        TopMoviesResponse data = movieAnalyticsService.getTopMovies(metric, limit, direction, startDate, endDate);
        return ApiResponse.success("Top movies retrieved successfully", data);
    }

    @GetMapping("/{movieId}")
    @Operation(summary = "Get movie revenue detail", description = "Retrieves aggregated statistics detail of a movie.")
    public ApiResponse<MovieRevenueDetailResponse> getMovieRevenueDetail(
            @PathVariable("movieId") Long movieId,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {

        MovieRevenueDetailResponse data = movieAnalyticsService.getMovieRevenueDetail(movieId, startDate, endDate);
        return ApiResponse.success("Movie revenue statistics retrieved successfully", data);
    }

    @GetMapping("/{movieId}/trend")
    @Operation(summary = "Get movie revenue trend", description = "Retrieves timeline trend of a movie within a date range.")
    public ApiResponse<MovieRevenueTrendResponse> getMovieRevenueTrend(
            @PathVariable("movieId") Long movieId,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam(value = "includeEmptyDates", required = false, defaultValue = "true") Boolean includeEmptyDates) {

        MovieRevenueTrendResponse data = movieAnalyticsService.getMovieRevenueTrend(movieId, startDate, endDate, includeEmptyDates);
        return ApiResponse.success("Movie revenue trend retrieved successfully", data);
    }
}
