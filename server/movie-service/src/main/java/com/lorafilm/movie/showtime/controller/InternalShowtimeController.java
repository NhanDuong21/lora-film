package com.lorafilm.movie.showtime.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.showtime.dto.request.BookingContextRequest;
import com.lorafilm.movie.showtime.dto.response.BookingContextResponse;
import com.lorafilm.movie.showtime.service.ShowtimeBookingContextService;
import com.lorafilm.movie.showtime.service.ShowtimeQueryService;
import com.lorafilm.movie.showtime.dto.SeatLayoutDto;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/internal/showtimes")
@Validated
public class InternalShowtimeController {

    private final ShowtimeBookingContextService showtimeBookingContextService;
    private final ShowtimeQueryService showtimeQueryService;

    public InternalShowtimeController(ShowtimeBookingContextService showtimeBookingContextService, ShowtimeQueryService showtimeQueryService) {
        this.showtimeBookingContextService = showtimeBookingContextService;
        this.showtimeQueryService = showtimeQueryService;
    }

    @PostMapping("/{showtimeId}/booking-context")
    public ResponseEntity<ApiResponse<BookingContextResponse>> getBookingContext(
            @PathVariable Long showtimeId,
            @Valid @RequestBody BookingContextRequest request) {

        BookingContextResponse response = showtimeBookingContextService.getBookingContext(showtimeId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{showtimeId}/seat-layout")
    public ResponseEntity<ApiResponse<SeatLayoutDto>> getSeatLayout(@PathVariable Long showtimeId) {
        SeatLayoutDto response = showtimeQueryService.getSeatLayout(showtimeId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
