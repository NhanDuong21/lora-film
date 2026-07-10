package com.lorafilm.movie.seat.controller;

import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.seat.dto.CreateSeatTypeRequest;
import com.lorafilm.movie.seat.dto.SeatTypeResponse;
import com.lorafilm.movie.seat.dto.UpdateSeatTypeRequest;

import com.lorafilm.movie.seat.service.SeatTypeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/seat-types")
public class AdminSeatTypeController {

    private final SeatTypeService seatTypeService;

    public AdminSeatTypeController(SeatTypeService seatTypeService) {
        this.seatTypeService = seatTypeService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SeatTypeResponse>> createSeatType(
            @Valid @RequestBody CreateSeatTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(seatTypeService.createSeatType(request)));
    }

    @PutMapping("/{seatTypePublicId}")
    public ResponseEntity<ApiResponse<SeatTypeResponse>> updateSeatType(
            @PathVariable String seatTypePublicId,
            @Valid @RequestBody UpdateSeatTypeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(seatTypeService.updateSeatType(seatTypePublicId, request)));
    }
}
