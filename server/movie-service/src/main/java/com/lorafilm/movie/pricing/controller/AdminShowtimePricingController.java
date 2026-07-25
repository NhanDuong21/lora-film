package com.lorafilm.movie.pricing.controller;

import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.pricing.dto.request.UpdateShowtimePricesRequest;
import com.lorafilm.movie.pricing.dto.response.ShowtimePricesResponse;
import com.lorafilm.movie.pricing.service.ShowtimePricingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/showtimes/{showtimeId}")
public class AdminShowtimePricingController {

    private final ShowtimePricingService showtimePricingService;

    public AdminShowtimePricingController(ShowtimePricingService showtimePricingService) {
        this.showtimePricingService = showtimePricingService;
    }

    @GetMapping("/prices")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<ShowtimePricesResponse>> getPrices(@PathVariable("showtimeId") String showtimeId) {
        return ResponseEntity.ok(ApiResponse.ok(showtimePricingService.getPrices(showtimeId)));
    }

    @PutMapping("/prices")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<ShowtimePricesResponse>> updatePrices(
            @PathVariable("showtimeId") String showtimeId,
            @Valid @RequestBody UpdateShowtimePricesRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(showtimePricingService.updatePrices(showtimeId, request)));
    }

    @GetMapping("/pricing")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<ShowtimePricesResponse>> getPricing(
            @PathVariable("showtimeId") String showtimeId) {
        return ResponseEntity.ok(ApiResponse.ok(showtimePricingService.getPrices(showtimeId)));
    }

    @PostMapping("/pricing/resolve")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<ShowtimePricesResponse>> resolvePricing(
            @PathVariable("showtimeId") String showtimeId,
            @Valid @RequestBody ResolveShowtimePricingRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                showtimePricingService.resolvePrices(showtimeId, request.expectedShowtimeVersion())));
    }

    public record ResolveShowtimePricingRequest(@NotNull Long expectedShowtimeVersion) {
    }
}
