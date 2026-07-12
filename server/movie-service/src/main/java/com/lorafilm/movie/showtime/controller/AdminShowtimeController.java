package com.lorafilm.movie.showtime.controller;

import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.showtime.dto.request.CreateShowtimeRequest;
import com.lorafilm.movie.showtime.dto.request.UpdateShowtimeRequest;
import com.lorafilm.movie.showtime.dto.response.AdminShowtimeResponse;
import com.lorafilm.movie.showtime.service.ShowtimeCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/showtimes")
@Tag(name = "Admin Showtime Command API", description = "Admin endpoints for managing showtimes")
public class AdminShowtimeController {

    private final ShowtimeCommandService showtimeCommandService;

    public AdminShowtimeController(ShowtimeCommandService showtimeCommandService) {
        this.showtimeCommandService = showtimeCommandService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Create a new showtime")
    public ResponseEntity<ApiResponse<AdminShowtimeResponse>> createShowtime(
            @Valid @RequestBody CreateShowtimeRequest request) {
        
        AdminShowtimeResponse response = showtimeCommandService.createShowtime(request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "SUCCESS", "Showtime created successfully", response, java.util.Collections.emptyList()));
    }

    @PutMapping("/{showtimePublicId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Update an existing draft showtime")
    public ResponseEntity<ApiResponse<AdminShowtimeResponse>> updateShowtime(
            @PathVariable String showtimePublicId,
            @Valid @RequestBody UpdateShowtimeRequest request) {
        
        AdminShowtimeResponse response = showtimeCommandService.updateShowtime(showtimePublicId, request);
        
        return ResponseEntity.ok(new ApiResponse<>(true, "SUCCESS", "Showtime updated successfully", response, java.util.Collections.emptyList()));
    }
}
