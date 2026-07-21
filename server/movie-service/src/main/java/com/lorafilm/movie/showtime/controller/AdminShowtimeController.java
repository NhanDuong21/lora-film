package com.lorafilm.movie.showtime.controller;

import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.showtime.dto.request.CreateShowtimeRequest;
import com.lorafilm.movie.showtime.dto.request.UpdateShowtimeRequest;
import com.lorafilm.movie.showtime.dto.request.UpdateShowtimeStatusRequest;
import com.lorafilm.movie.showtime.dto.response.AdminShowtimeResponse;
import com.lorafilm.movie.showtime.dto.response.ShowtimeStatusHistoryResponse;
import com.lorafilm.movie.showtime.service.ShowtimeCommandService;
import com.lorafilm.movie.showtime.service.ShowtimeStatusHistoryService;
import com.lorafilm.movie.showtime.service.ShowtimeStatusTransitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/showtimes")
@Tag(name = "Admin Showtime Command API", description = "Admin endpoints for managing showtimes")
public class AdminShowtimeController {

    private final ShowtimeCommandService showtimeCommandService;
    private final ShowtimeStatusTransitionService transitionService;
    private final ShowtimeStatusHistoryService historyService;

    public AdminShowtimeController(ShowtimeCommandService showtimeCommandService,
                                   ShowtimeStatusTransitionService transitionService,
                                   ShowtimeStatusHistoryService historyService) {
        this.showtimeCommandService = showtimeCommandService;
        this.transitionService = transitionService;
        this.historyService = historyService;
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

    @PutMapping("/{showtimePublicId}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Transition showtime status")
    public ResponseEntity<ApiResponse<AdminShowtimeResponse>> transitionStatus(
            @PathVariable String showtimePublicId,
            @Valid @RequestBody UpdateShowtimeStatusRequest request) {
        
        AdminShowtimeResponse response = transitionService.transitionStatus(showtimePublicId, request);
        
        return ResponseEntity.ok(new ApiResponse<>(true, "SUCCESS", "Showtime status updated successfully", response, java.util.Collections.emptyList()));
    }

    @GetMapping("/{showtimePublicId}/status-history")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Get showtime status history")
    public ResponseEntity<ApiResponse<List<ShowtimeStatusHistoryResponse>>> getStatusHistory(
            @PathVariable String showtimePublicId) {
        
        List<ShowtimeStatusHistoryResponse> response = historyService.getShowtimeStatusHistory(showtimePublicId);
        
        return ResponseEntity.ok(new ApiResponse<>(true, "SUCCESS", "Showtime status history retrieved successfully", response, java.util.Collections.emptyList()));
    }

    @PutMapping("/batch/{batchId}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Transition status for a batch of showtimes")
    public ResponseEntity<ApiResponse<Void>> transitionBatchStatus(
            @PathVariable String batchId,
            @Valid @RequestBody UpdateShowtimeStatusRequest request) {
        
        transitionService.transitionBatchStatus(batchId, request);
        
        return ResponseEntity.ok(new ApiResponse<>(true, "SUCCESS", "Batch status updated successfully", null, java.util.Collections.emptyList()));
    }

    @DeleteMapping("/batch/{batchId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Delete a batch of DRAFT showtimes")
    public ResponseEntity<ApiResponse<Void>> deleteBatch(
            @PathVariable String batchId) {
        
        showtimeCommandService.deleteBatch(batchId);
        
        return ResponseEntity.ok(new ApiResponse<>(true, "SUCCESS", "Batch deleted successfully", null, java.util.Collections.emptyList()));
    }
}
