package com.lorafilm.movie.showtime.controller;

import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.showtime.dto.request.UpdateShowtimeBlockedSeatsRequest;
import com.lorafilm.movie.showtime.dto.response.ShowtimeSeatControlResponse;
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
@RequestMapping("/api/admin/showtimes")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminShowtimeSeatController {

    private final ShowtimeSeatBlockingService seatBlockingService;

    public AdminShowtimeSeatController(ShowtimeSeatBlockingService seatBlockingService) {
        this.seatBlockingService = seatBlockingService;
    }

    @GetMapping("/{showtimePublicId}/seat-control")
    public ResponseEntity<ApiResponse<ShowtimeSeatControlResponse>> getSeatControl(
            @PathVariable String showtimePublicId) {
        return ResponseEntity.ok(ApiResponse.ok(
                seatBlockingService.getSeatControl(showtimePublicId)));
    }

    @PostMapping("/{showtimePublicId}/blocked-seats")
    public ResponseEntity<ApiResponse<ShowtimeSeatControlResponse>> blockSeats(
            @PathVariable String showtimePublicId,
            @Valid @RequestBody UpdateShowtimeBlockedSeatsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                seatBlockingService.blockSeats(showtimePublicId, request)));
    }

    @PutMapping("/{showtimePublicId}/blocked-seats/release")
    public ResponseEntity<ApiResponse<ShowtimeSeatControlResponse>> releaseSeats(
            @PathVariable String showtimePublicId,
            @Valid @RequestBody UpdateShowtimeBlockedSeatsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                seatBlockingService.releaseSeats(showtimePublicId, request)));
    }
}
