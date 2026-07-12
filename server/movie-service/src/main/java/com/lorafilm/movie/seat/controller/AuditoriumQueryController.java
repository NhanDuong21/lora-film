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

    @io.swagger.v3.oas.annotations.Operation(summary = "Get customer seat layout")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.lorafilm.movie.common.api.error.NotFoundErrorResponse.class)))
    })
    @GetMapping("/api/auditoriums/{id}/seat-layout")
    public ResponseEntity<ApiResponse<CustomerSeatLayoutResponse>> getCustomerSeatLayout(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(queryService.getCustomerSeatLayout(id)));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Get admin seat layout")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.lorafilm.movie.common.api.error.NotFoundErrorResponse.class)))
    })
    @GetMapping("/api/admin/auditoriums/{id}/seat-layout")
    public ResponseEntity<ApiResponse<AdminSeatLayoutResponse>> getAdminSeatLayout(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(queryService.getAdminSeatLayout(id)));
    }
}
