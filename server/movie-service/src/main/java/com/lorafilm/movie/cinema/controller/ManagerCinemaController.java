package com.lorafilm.movie.cinema.controller;

import com.lorafilm.movie.auditorium.dto.CreateMaintenanceWindowRequest;
import com.lorafilm.movie.auditorium.dto.MaintenanceWindowResponse;
import com.lorafilm.movie.auditorium.service.AuditoriumMaintenanceService;
import com.lorafilm.movie.cinema.dto.CinemaDetailDto;
import com.lorafilm.movie.cinema.dto.OperatingHourResponse;
import com.lorafilm.movie.cinema.dto.OperatingHourUpdateRequest;
import com.lorafilm.movie.cinema.service.CinemaService;
import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.common.dto.PageResponse;
import com.lorafilm.movie.common.security.ManagerCinemaScopeService;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeSource;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.dto.request.UpdateShowtimeStatusRequest;
import com.lorafilm.movie.showtime.dto.response.AdminShowtimeResponse;
import com.lorafilm.movie.showtime.dto.response.ShowtimeStatusHistoryResponse;
import com.lorafilm.movie.showtime.service.AdminShowtimeQueryService;
import com.lorafilm.movie.showtime.service.ShowtimeStatusHistoryService;
import com.lorafilm.movie.showtime.service.ShowtimeStatusTransitionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
    private final ShowtimeStatusTransitionService transitionService;
    private final ShowtimeStatusHistoryService historyService;
    private final AuditoriumMaintenanceService maintenanceService;
    private final ManagerCinemaScopeService cinemaScope;

    public ManagerCinemaController(CinemaService cinemaService,
                                   AdminShowtimeQueryService showtimeQueryService,
                                   ShowtimeStatusTransitionService transitionService,
                                   ShowtimeStatusHistoryService historyService,
                                   AuditoriumMaintenanceService maintenanceService,
                                   ManagerCinemaScopeService cinemaScope) {
        this.cinemaService = cinemaService;
        this.showtimeQueryService = showtimeQueryService;
        this.transitionService = transitionService;
        this.historyService = historyService;
        this.maintenanceService = maintenanceService;
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

    @GetMapping("/showtimes/{showtimePublicId}")
    public ResponseEntity<ApiResponse<AdminShowtimeResponse>> getShowtime(
            @PathVariable String showtimePublicId) {
        return ResponseEntity.ok(ApiResponse.ok(requireAssignedShowtime(showtimePublicId)));
    }

    @PutMapping("/showtimes/{showtimePublicId}/status")
    public ResponseEntity<ApiResponse<AdminShowtimeResponse>> transitionShowtimeStatus(
            @PathVariable String showtimePublicId,
            @Valid @RequestBody UpdateShowtimeStatusRequest request) {
        requireAssignedShowtime(showtimePublicId);
        return ResponseEntity.ok(ApiResponse.ok(
                transitionService.transitionStatus(showtimePublicId, request)));
    }

    @GetMapping("/showtimes/{showtimePublicId}/status-history")
    public ResponseEntity<ApiResponse<List<ShowtimeStatusHistoryResponse>>> getShowtimeStatusHistory(
            @PathVariable String showtimePublicId) {
        requireAssignedShowtime(showtimePublicId);
        return ResponseEntity.ok(ApiResponse.ok(
                historyService.getShowtimeStatusHistory(showtimePublicId)));
    }

    @GetMapping("/cinemas/{cinemaPublicId}/maintenance-windows")
    public ResponseEntity<ApiResponse<List<MaintenanceWindowResponse>>> getMaintenanceWindows(
            @PathVariable String cinemaPublicId) {
        CinemaDetailDto cinema = requireAssignedCinema(cinemaPublicId);
        List<MaintenanceWindowResponse> windows = cinema.getActiveAuditoriums().stream()
                .flatMap(auditorium -> maintenanceService
                        .getMaintenanceWindows(auditorium.getPublicId()).stream())
                .sorted(java.util.Comparator.comparing(MaintenanceWindowResponse::startTime).reversed())
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(windows));
    }

    @PostMapping("/cinemas/{cinemaPublicId}/auditoriums/{auditoriumPublicId}/maintenance-windows")
    public ResponseEntity<ApiResponse<MaintenanceWindowResponse>> createMaintenanceWindow(
            @PathVariable String cinemaPublicId,
            @PathVariable String auditoriumPublicId,
            @Valid @RequestBody CreateMaintenanceWindowRequest request) {
        requireAssignedAuditorium(cinemaPublicId, auditoriumPublicId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                maintenanceService.createWindow(auditoriumPublicId, request)));
    }

    @PutMapping("/cinemas/{cinemaPublicId}/maintenance-windows/{maintenanceWindowId}/cancel")
    public ResponseEntity<ApiResponse<MaintenanceWindowResponse>> cancelMaintenanceWindow(
            @PathVariable String cinemaPublicId,
            @PathVariable Long maintenanceWindowId) {
        CinemaDetailDto cinema = requireAssignedCinema(cinemaPublicId);
        boolean belongsToCinema = cinema.getActiveAuditoriums().stream()
                .flatMap(auditorium -> maintenanceService
                        .getMaintenanceWindows(auditorium.getPublicId()).stream())
                .anyMatch(window -> maintenanceWindowId.equals(window.id()));
        if (!belongsToCinema) {
            throw denied("Không thể thao tác lịch bảo trì không thuộc rạp được phân công.");
        }
        return ResponseEntity.ok(ApiResponse.ok(
                maintenanceService.cancelWindow(maintenanceWindowId)));
    }

    private AdminShowtimeResponse requireAssignedShowtime(String showtimePublicId) {
        AdminShowtimeResponse showtime = showtimeQueryService.getAdminShowtimeByPublicId(showtimePublicId);
        if (showtime.getCinema() == null) {
            throw denied("Không xác định được rạp của suất chiếu này.");
        }
        cinemaScope.requireAssigned(showtime.getCinema().getPublicId());
        return showtime;
    }

    private CinemaDetailDto requireAssignedCinema(String cinemaPublicId) {
        cinemaScope.requireAssigned(cinemaPublicId);
        return cinemaService.getAdminCinemaDetail(cinemaPublicId);
    }

    private void requireAssignedAuditorium(String cinemaPublicId, String auditoriumPublicId) {
        CinemaDetailDto cinema = requireAssignedCinema(cinemaPublicId);
        boolean belongsToCinema = cinema.getActiveAuditoriums().stream()
                .anyMatch(auditorium -> auditoriumPublicId.equalsIgnoreCase(auditorium.getPublicId()));
        if (!belongsToCinema) {
            throw denied("Phòng chiếu không thuộc rạp được phân công.");
        }
    }

    private com.lorafilm.movie.common.exception.BusinessException denied(String message) {
        return new com.lorafilm.movie.common.exception.BusinessException(
                com.lorafilm.movie.common.exception.ErrorCode.ACCESS_DENIED, message);
    }
}
