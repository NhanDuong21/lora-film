package com.lorafilm.movie.showtime.controller;

import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.common.dto.PageResponse;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeSource;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.dto.response.AdminShowtimeResponse;
import com.lorafilm.movie.showtime.service.AdminShowtimeQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/showtimes")
@Validated
@Tag(name = "Admin Showtime Query API", description = "Admin endpoints for querying showtimes")
public class AdminShowtimeQueryController {

    private final AdminShowtimeQueryService queryService;

    public AdminShowtimeQueryController(AdminShowtimeQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Get paginated list of showtimes for admin")
    public ResponseEntity<ApiResponse<PageResponse<AdminShowtimeResponse>>> getAdminShowtimes(
            @RequestParam(required = false) String cinemaSlug,
            @RequestParam(required = false) String movieSlug,
            @RequestParam(required = false) ShowtimeStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String batchId,
            @RequestParam(required = false) ShowtimeSource source,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        
        PageResponse<AdminShowtimeResponse> response = queryService.getAdminShowtimes(
                cinemaSlug, movieSlug, status, date, batchId, source, page, size);
                
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{showtimePublicId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Get detailed information of a showtime for admin")
    public ResponseEntity<ApiResponse<AdminShowtimeResponse>> getAdminShowtimeDetail(
            @PathVariable String showtimePublicId) {
        
        AdminShowtimeResponse response = queryService.getAdminShowtimeByPublicId(showtimePublicId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
