package com.lorafilm.movie.movie.controller;

import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.common.dto.PageResponse;
import com.lorafilm.movie.movie.dto.MovieDto;
import com.lorafilm.movie.movie.dto.AdminMovieListQuery;
import com.lorafilm.movie.movie.dto.MovieBulkApprovalResponse;
import com.lorafilm.movie.movie.dto.TmdbQueueBreakdownResponse;
import com.lorafilm.movie.movie.dto.MovieDetailDto;
import com.lorafilm.movie.movie.dto.MovieGenreAssignRequest;
import com.lorafilm.movie.movie.dto.MovieRequest;
import com.lorafilm.movie.movie.service.AdminMovieService;
import com.lorafilm.movie.movie.service.MovieService;
import com.lorafilm.movie.movie.service.MovieSummaryQueryService;
import com.lorafilm.movie.movie.dto.MovieSummaryResponse;
import com.lorafilm.movie.movie.dto.MovieStatusHistoryResponse;
import com.lorafilm.movie.movie.service.MovieStatusHistoryService;
import com.lorafilm.movie.movie.dto.MovieLaunchReadinessResponse;
import com.lorafilm.movie.movie.service.MovieLaunchReadinessService;
import com.lorafilm.movie.movie.service.MovieExhibitionPeriodService;
import com.lorafilm.movie.movie.dto.CreateMovieExhibitionPeriodRequest;
import com.lorafilm.movie.movie.dto.MovieExhibitionPeriodResponse;
import com.lorafilm.movie.integration.tmdb.dto.TmdbMovieReviewResponse;
import com.lorafilm.movie.integration.tmdb.service.TmdbMovieReviewService;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/movies")
@Validated
public class AdminMovieController {

    private final AdminMovieService adminMovieService;
    private final MovieService movieService;
    private final MovieSummaryQueryService movieSummaryQueryService;
    private final TmdbMovieReviewService tmdbMovieReviewService;
    private final MovieStatusHistoryService movieStatusHistoryService;
    private final MovieLaunchReadinessService movieLaunchReadinessService;
    private final MovieExhibitionPeriodService movieExhibitionPeriodService;

    public AdminMovieController(
            AdminMovieService adminMovieService,
            MovieService movieService,
            MovieSummaryQueryService movieSummaryQueryService,
            TmdbMovieReviewService tmdbMovieReviewService,
            MovieStatusHistoryService movieStatusHistoryService,
            MovieLaunchReadinessService movieLaunchReadinessService,
            MovieExhibitionPeriodService movieExhibitionPeriodService) {
        this.adminMovieService = adminMovieService;
        this.movieService = movieService;
        this.movieSummaryQueryService = movieSummaryQueryService;
        this.tmdbMovieReviewService = tmdbMovieReviewService;
        this.movieStatusHistoryService = movieStatusHistoryService;
        this.movieLaunchReadinessService = movieLaunchReadinessService;
        this.movieExhibitionPeriodService = movieExhibitionPeriodService;
    }

    @PostMapping
    public ApiResponse<MovieDto> createMovie(@Valid @RequestBody MovieRequest request) {
        return ApiResponse.ok(adminMovieService.createMovie(request));
    }

    @PutMapping("/{publicId}")
    public ApiResponse<MovieDto> updateMovie(
            @PathVariable String publicId, 
            @Valid @RequestBody MovieRequest request) {
        return ApiResponse.ok(adminMovieService.updateMovie(publicId, request));
    }

    @PostMapping("/{publicId}/genres")
    public ApiResponse<String> assignGenresPost(
            @PathVariable String publicId,
            @Valid @RequestBody MovieGenreAssignRequest request) {
        adminMovieService.appendGenres(publicId, request.getGenreIds());
        return ApiResponse.ok("Genres appended successfully");
    }

    @PutMapping("/{publicId}/genres")
    public ApiResponse<String> assignGenresPut(
            @PathVariable String publicId,
            @Valid @RequestBody MovieGenreAssignRequest request) {
        adminMovieService.assignGenres(publicId, request.getGenreIds());
        return ApiResponse.ok("Genres updated successfully");
    }

    @PostMapping("/{publicId}/credits")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<String> assignCreditsPost(
            @PathVariable("publicId") String publicId,
            @Valid @RequestBody com.lorafilm.movie.movie.dto.MovieCreditAssignRequest request) {
        adminMovieService.appendCredits(publicId, request.getCredits());
        return ApiResponse.ok("Credits appended successfully");
    }

    @PutMapping("/{publicId}/credits")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<String> assignCreditsPut(
            @PathVariable("publicId") String publicId,
            @Valid @RequestBody com.lorafilm.movie.movie.dto.MovieCreditAssignRequest request) {
        adminMovieService.assignCredits(publicId, request.getCredits());
        return ApiResponse.ok("Credits updated successfully");
    }

    @PostMapping("/{publicId}/production-companies")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<String> assignProductionCompaniesPost(
            @PathVariable("publicId") String publicId,
            @Valid @RequestBody com.lorafilm.movie.movie.dto.MovieCompanyAssignRequest request) {
        adminMovieService.appendProductionCompanies(publicId, request.getCompanies());
        return ApiResponse.ok("Production companies appended successfully");
    }

    @PutMapping("/{publicId}/production-companies")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<String> assignProductionCompaniesPut(
            @PathVariable("publicId") String publicId,
            @Valid @RequestBody com.lorafilm.movie.movie.dto.MovieCompanyAssignRequest request) {
        adminMovieService.assignProductionCompanies(publicId, request.getCompanies());
        return ApiResponse.ok("Production companies updated successfully");
    }

    @DeleteMapping("/{publicId}")
    public ApiResponse<String> deleteMovie(@PathVariable String publicId) {
        adminMovieService.deleteMovie(publicId);
        return ApiResponse.ok("Movie deleted successfully");
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<MovieDto>>> getMovies(
            @Valid @ModelAttribute AdminMovieListQuery query) {
        return ResponseEntity.ok(ApiResponse.ok(movieService.getMovies(query)));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<MovieSummaryResponse>> getMovieSummary() {
        return ResponseEntity.ok(ApiResponse.ok(movieSummaryQueryService.getSummary()));
    }

    @PostMapping("/bulk-approve")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<MovieBulkApprovalResponse>> bulkApproveTmdbMovies(
            @Valid @RequestBody AdminMovieListQuery filter,
            @RequestParam(name = "limit", defaultValue = "100") @Min(1) @Max(100) int limit) {
        return ResponseEntity.ok(ApiResponse.ok(movieService.bulkApproveTmdbMovies(filter, limit)));
    }

    @GetMapping("/tmdb-queue-breakdown")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<TmdbQueueBreakdownResponse>> getTmdbQueueBreakdown(
            @Valid @ModelAttribute AdminMovieListQuery filter) {
        return ResponseEntity.ok(ApiResponse.ok(movieService.getTmdbQueueBreakdown(filter)));
    }

    @GetMapping("/{publicId}/tmdb-review")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<TmdbMovieReviewResponse>> getTmdbReview(
            @PathVariable("publicId") String publicId) {
        return ResponseEntity.ok(ApiResponse.ok(tmdbMovieReviewService.getReview(publicId)));
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<MovieDetailDto>> getMovieDetail(@PathVariable("publicId") String publicId) {
        return ResponseEntity.ok(ApiResponse.ok(movieService.getMovieByIdentifier(publicId)));
    }

    @PutMapping("/{publicId}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<MovieDto>> updateMovieStatus(
            @PathVariable("publicId") String publicId,
            @RequestParam("status") MovieStatus status,
            @RequestParam(value = "reason", required = false) @Size(max = 500) String reason) {
        return ResponseEntity.ok(ApiResponse.ok(movieService.updateMovieStatus(publicId, status, reason)));
    }

    @GetMapping("/{publicId}/status-history")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<java.util.List<MovieStatusHistoryResponse>>> getStatusHistory(
            @PathVariable("publicId") String publicId) {
        return ResponseEntity.ok(ApiResponse.ok(movieStatusHistoryService.getHistory(publicId)));
    }

    @GetMapping("/{publicId}/launch-readiness")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<MovieLaunchReadinessResponse>> getLaunchReadiness(
            @PathVariable("publicId") String publicId) {
        return ResponseEntity.ok(ApiResponse.ok(movieLaunchReadinessService.get(publicId)));
    }

    @GetMapping("/{publicId}/exhibition-periods")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<java.util.List<MovieExhibitionPeriodResponse>>> getExhibitionPeriods(
            @PathVariable("publicId") String publicId) {
        return ResponseEntity.ok(ApiResponse.ok(movieExhibitionPeriodService.getPeriods(publicId)));
    }

    @PostMapping("/{publicId}/exhibition-periods")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<MovieExhibitionPeriodResponse>> createExhibitionPeriod(
            @PathVariable("publicId") String publicId,
            @Valid @RequestBody CreateMovieExhibitionPeriodRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(movieExhibitionPeriodService.createPeriod(publicId, request)));
    }
}
