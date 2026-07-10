package com.lorafilm.movie.showtime.controller;

import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.common.dto.PageResponse;
import com.lorafilm.movie.showtime.dto.SeatLayoutDto;
import com.lorafilm.movie.showtime.dto.ShowtimeDto;
import com.lorafilm.movie.showtime.service.ShowtimeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/showtimes")
public class ShowtimeController {

    private final ShowtimeService showtimeService;

    public ShowtimeController(ShowtimeService showtimeService) {
        this.showtimeService = showtimeService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ShowtimeDto>>> getShowtimes(
            @RequestParam(required = false) String movieSlug,
            @RequestParam(required = false) String cinemaSlug,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(showtimeService.getShowtimes(movieSlug, cinemaSlug, city, date, page, size)));
    }

    @GetMapping("/{showtimePublicId}")
    public ResponseEntity<ApiResponse<ShowtimeDto>> getShowtimeDetail(@PathVariable String showtimePublicId) {
        return ResponseEntity.ok(ApiResponse.ok(showtimeService.getShowtimeByPublicId(showtimePublicId)));
    }

    @GetMapping("/{showtimePublicId}/seat-layout")
    public ResponseEntity<ApiResponse<SeatLayoutDto>> getSeatLayout(@PathVariable String showtimePublicId) {
        return ResponseEntity.ok(ApiResponse.ok(showtimeService.getSeatLayout(showtimePublicId)));
    }
}
