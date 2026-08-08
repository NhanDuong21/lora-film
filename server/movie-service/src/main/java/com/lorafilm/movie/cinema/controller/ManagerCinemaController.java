package com.lorafilm.movie.cinema.controller;

import com.lorafilm.movie.cinema.dto.CinemaDetailDto;
import com.lorafilm.movie.cinema.dto.OperatingHourResponse;
import com.lorafilm.movie.cinema.dto.OperatingHourUpdateRequest;
import com.lorafilm.movie.cinema.service.CinemaService;
import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.common.dto.PageResponse;
import com.lorafilm.movie.common.security.ManagerCinemaScopeService;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeSource;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.dto.response.AdminShowtimeResponse;
import com.lorafilm.movie.showtime.service.AdminShowtimeQueryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager")
@Validated
@PreAuthorize("hasAuthority('ROLE_MANAGER')")
public class ManagerCinemaController {

    private final CinemaService cinemaService;
    private final AdminShowtimeQueryService showtimeQueryService;
    private final ManagerCinemaScopeService cinemaScope;

    public ManagerCinemaController(CinemaService cinemaService,
                                   AdminShowtimeQueryService showtimeQueryService,
                                   ManagerCinemaScopeService cinemaScope) {
        this.cinemaService = cinemaService;
        this.showtimeQueryService = showtimeQueryService;
        this.cinemaScope = cinemaScope;
    }

    @GetMapping("/cinemas")
    public ResponseEntity<ApiResponse<List<CinemaDetailDto>>> getAssignedCinemas() {
        List<CinemaDetailDto> cinemas = cinemaScope.getAssignedCinemaPublicIds().stream()
                .map(cinemaService::getAdminCinemaDetail)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(cinemas));
    }

    @GetMapping("/cinemas/{cinemaPublicId}")
    public ResponseEntity<ApiResponse<CinemaDetailDto>> getAssignedCinema(
            @PathVariable String cinemaPublicId) {
        cinemaScope.requireAssigned(cinemaPublicId);
        return ResponseEntity.ok(ApiResponse.ok(
                cinemaService.getAdminCinemaDetail(cinemaPublicId)));
    }

    @PutMapping("/cinemas/{cinemaPublicId}/operating-hours")
    public ResponseEntity<ApiResponse<List<OperatingHourResponse>>> updateOperatingHours(
            @PathVariable String cinemaPublicId,
            @Valid @RequestBody List<OperatingHourUpdateRequest> requests) {
        cinemaScope.requireAssigned(cinemaPublicId);
        return ResponseEntity.ok(ApiResponse.ok(
                cinemaService.updateOperatingHours(cinemaPublicId, requests)));
    }

    @GetMapping("/showtimes")
    public ResponseEntity<ApiResponse<PageResponse<AdminShowtimeResponse>>> getShowtimes(
            @RequestParam String cinemaPublicId,
            @RequestParam(required = false) String movieSlug,
            @RequestParam(required = false) ShowtimeStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String batchId,
            @RequestParam(required = false) ShowtimeSource source,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        cinemaScope.requireAssigned(cinemaPublicId);
        CinemaDetailDto cinema = cinemaService.getAdminCinemaDetail(cinemaPublicId);
        PageResponse<AdminShowtimeResponse> response = showtimeQueryService.getAdminShowtimes(
                cinema.getSlug(), movieSlug, status, date, batchId, source, page, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
