package com.lorafilm.movie.seat.controller;

import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.seat.dto.AdminSeatLayoutResponse;
import com.lorafilm.movie.seat.dto.CustomerSeatLayoutResponse;
import com.lorafilm.movie.seat.service.SeatLayoutQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditoriumQueryController {

    private final SeatLayoutQueryService queryService;

    public AuditoriumQueryController(SeatLayoutQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/api/auditoriums/{id}/seat-layout")
    public ResponseEntity<ApiResponse<CustomerSeatLayoutResponse>> getCustomerSeatLayout(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(queryService.getCustomerSeatLayout(id)));
    }

    @GetMapping("/api/admin/auditoriums/{id}/seat-layout")
    public ResponseEntity<ApiResponse<AdminSeatLayoutResponse>> getAdminSeatLayout(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(queryService.getAdminSeatLayout(id)));
    }
}
