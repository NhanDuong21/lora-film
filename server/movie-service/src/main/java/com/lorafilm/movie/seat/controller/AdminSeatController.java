package com.lorafilm.movie.seat.controller;

import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.seat.dto.BulkCreateSeatsRequest;
import com.lorafilm.movie.seat.dto.SeatResponse;
import com.lorafilm.movie.seat.dto.UpdateSeatRequest;

import com.lorafilm.movie.seat.service.SeatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminSeatController {

    private final SeatService seatService;

    public AdminSeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    @PostMapping("/auditoriums/{auditoriumPublicId}/seats/bulk")
    public ResponseEntity<ApiResponse<List<SeatResponse>>> bulkCreateSeats(
            @PathVariable String auditoriumPublicId,
            @Valid @RequestBody BulkCreateSeatsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(seatService.bulkCreateSeats(auditoriumPublicId, request)));
    }

    @PutMapping("/seats/{seatPublicId}")
    public ResponseEntity<ApiResponse<SeatResponse>> updateSeat(
            @PathVariable String seatPublicId,
            @Valid @RequestBody UpdateSeatRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(seatService.updateSeat(seatPublicId, request)));
    }
}
