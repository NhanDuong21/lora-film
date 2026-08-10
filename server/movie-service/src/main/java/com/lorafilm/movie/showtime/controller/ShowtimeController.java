package com.lorafilm.movie.showtime.controller;

import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.common.dto.PageResponse;
import com.lorafilm.movie.showtime.dto.SeatLayoutDto;
import com.lorafilm.movie.showtime.dto.ShowtimeDto;
import com.lorafilm.movie.showtime.service.ShowtimeQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/showtimes")
@Validated
public class ShowtimeController {

    private final ShowtimeQueryService showtimeQueryService;

    public ShowtimeController(ShowtimeQueryService showtimeQueryService) {
        this.showtimeQueryService = showtimeQueryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ShowtimeDto>>> getShowtimes(
            @RequestParam(required = false) String movieSlug,
            @RequestParam(required = false) String cinemaSlug,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) String format,
            @RequestParam(required = false) String audioLanguage,
            @RequestParam(required = false) String subtitleLanguage,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(ApiResponse.ok(showtimeQueryService.getShowtimes(movieSlug, cinemaSlug, city, date, format, audioLanguage, subtitleLanguage, page, size)));
    }

    @GetMapping("/{showtimePublicId}")
    public ResponseEntity<ApiResponse<ShowtimeDto>> getShowtimeDetail(@PathVariable String showtimePublicId) {
        return ResponseEntity.ok(ApiResponse.ok(showtimeQueryService.getShowtimeByPublicId(showtimePublicId)));
    }

    @GetMapping("/{showtimePublicId}/seat-layout")
    public ResponseEntity<ApiResponse<SeatLayoutDto>> getSeatLayout(@PathVariable String showtimePublicId) {
        return ResponseEntity.ok(ApiResponse.ok(showtimeQueryService.getSeatLayout(showtimePublicId)));
    }
}
