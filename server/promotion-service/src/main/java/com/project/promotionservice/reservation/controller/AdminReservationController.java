package com.project.promotionservice.reservation.controller;

import com.project.promotionservice.common.response.ApiResponse;
import com.project.promotionservice.common.response.PagedResponse;
import com.project.promotionservice.reservation.dto.response.ReservationResponse;
import com.project.promotionservice.reservation.enums.ReservationStatus;
import com.project.promotionservice.reservation.service.PromotionReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

import static com.project.promotionservice.common.constant.ValidationConstants.UUID_PATTERN;
import static com.project.promotionservice.common.constant.ValidationConstants.USER_REFERENCE_PATTERN;

@RestController
@Validated
@RequestMapping("/api/admin/reservations")
@PreAuthorize("hasAuthority('PROMOTION_AUDIT_VIEW')")
@Tag(name = "Admin Reservation APIs", description = "Reservation history and operational lookup")
@SecurityRequirement(name = "bearerAuth")
public class AdminReservationController {

    private final PromotionReservationService reservationService;

    public AdminReservationController(PromotionReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    @Operation(summary = "Search reservation history")
    public ResponseEntity<ApiResponse<PagedResponse<ReservationResponse>>> history(
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false)
            @Pattern(regexp = USER_REFERENCE_PATTERN,
                    message = "userPublicId must be a positive account ID or a valid UUID")
            String userPublicId,
            @RequestParam(required = false)
            @Pattern(regexp = UUID_PATTERN, message = "bookingPublicId must be a valid UUID")
            String bookingPublicId,
            @RequestParam(required = false)
            @Pattern(regexp = UUID_PATTERN, message = "orderPublicId must be a valid UUID")
            String orderPublicId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(ApiResponse.success(reservationService.history(
                status, userPublicId, bookingPublicId, orderPublicId,
                from, to, page, size)));
    }
}
