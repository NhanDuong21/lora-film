package com.lorafilm.movie.showtime.controller;

import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.security.ManagerCinemaScopeService;
import com.lorafilm.movie.showtime.dto.request.UpdateShowtimeBlockedSeatsRequest;
import com.lorafilm.movie.showtime.dto.response.AdminShowtimeResponse;
import com.lorafilm.movie.showtime.dto.response.ShowtimeSeatControlResponse;
import com.lorafilm.movie.showtime.service.AdminShowtimeQueryService;
import com.lorafilm.movie.showtime.service.ShowtimeSeatBlockingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager/showtimes")
@PreAuthorize("hasAuthority('ROLE_MANAGER')")
public class ManagerShowtimeSeatController {

    private final ShowtimeSeatBlockingService seatBlockingService;
    private final AdminShowtimeQueryService showtimeQueryService;
    private final ManagerCinemaScopeService cinemaScope;

    public ManagerShowtimeSeatController(
            ShowtimeSeatBlockingService seatBlockingService,
            AdminShowtimeQueryService showtimeQueryService,
            ManagerCinemaScopeService cinemaScope) {
        this.seatBlockingService = seatBlockingService;
        this.showtimeQueryService = showtimeQueryService;
        this.cinemaScope = cinemaScope;
    }

    @GetMapping("/{showtimePublicId}/seat-control")
    public ResponseEntity<ApiResponse<ShowtimeSeatControlResponse>> getSeatControl(
            @PathVariable String showtimePublicId) {
        requireAssigned(showtimePublicId);
        return ResponseEntity.ok(ApiResponse.ok(
                seatBlockingService.getSeatControl(showtimePublicId)));
    }

    @PostMapping("/{showtimePublicId}/blocked-seats")
    public ResponseEntity<ApiResponse<ShowtimeSeatControlResponse>> blockSeats(
            @PathVariable String showtimePublicId,
            @Valid @RequestBody UpdateShowtimeBlockedSeatsRequest request) {
        requireAssigned(showtimePublicId);
        return ResponseEntity.ok(ApiResponse.ok(
                seatBlockingService.blockSeats(showtimePublicId, request)));
    }

    @PutMapping("/{showtimePublicId}/blocked-seats/release")
    public ResponseEntity<ApiResponse<ShowtimeSeatControlResponse>> releaseSeats(
            @PathVariable String showtimePublicId,
            @Valid @RequestBody UpdateShowtimeBlockedSeatsRequest request) {
        requireAssigned(showtimePublicId);
        return ResponseEntity.ok(ApiResponse.ok(
                seatBlockingService.releaseSeats(showtimePublicId, request)));
    }

    private void requireAssigned(String showtimePublicId) {
        AdminShowtimeResponse showtime = showtimeQueryService.getAdminShowtimeByPublicId(showtimePublicId);
        if (showtime.getCinema() == null) {
            throw new BusinessException(
                    ErrorCode.ACCESS_DENIED,
                    "Không xác định được rạp của suất chiếu này.");
        }
        cinemaScope.requireAssigned(showtime.getCinema().getPublicId());
    }
}
