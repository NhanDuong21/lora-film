package com.lorafilm.movie.seat.controller;

import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.seat.dto.CreateSeatTypeRequest;
import com.lorafilm.movie.seat.dto.SeatTypeResponse;
import com.lorafilm.movie.seat.dto.UpdateSeatTypeRequest;
import com.lorafilm.movie.seat.dto.UpdateSeatTypeStatusRequest;
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

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SeatTypeResponse>> updateSeatType(
            @PathVariable String id,
            @Valid @RequestBody UpdateSeatTypeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(seatTypeService.updateSeatType(id, request)));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<SeatTypeResponse>> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateSeatTypeStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(seatTypeService.updateStatus(id, request)));
    }
}
