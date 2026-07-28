package com.project.promotionservice.reservation.controller;

import com.project.promotionservice.common.response.ApiResponse;
import com.project.promotionservice.common.response.ErrorResponse;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.ConfirmRequest;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.ReserveRequest;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.RollbackRequest;
import com.project.promotionservice.reservation.dto.response.ReservationResponse;
import com.project.promotionservice.reservation.service.PromotionReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/promotions")
@Tag(
        name = "Promotion Checkout APIs",
        description = "Canonical validate, reserve, confirm and rollback lifecycle for payment integration")
@SecurityRequirement(name = "internalTokenAuth")
@ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400", description = "Malformed request or invalid benefit configuration",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404", description = "Benefit or reservation was not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409", description = "Idempotency, capacity or lifecycle conflict",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class InternalPromotionReservationController {

    private final PromotionReservationService reservationService;

    public InternalPromotionReservationController(PromotionReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/reserve")
    @Operation(summary = "Validate and atomically reserve a coupon or voucher")
    public ResponseEntity<ApiResponse<ReservationResponse>> reserve(
            @Parameter(required = true, description = "Stable key for safe request retries")
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ReserveRequest request) {
        ReservationResponse response = reservationService.reserve(request, idempotencyKey, "SYSTEM");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Promotion reserved successfully", response));
    }

    @PostMapping("/confirm")
    @Operation(summary = "Confirm payment and create the final redemption ledger entry")
    public ResponseEntity<ApiResponse<ReservationResponse>> confirm(
            @Parameter(required = true, description = "Stable key for safe request retries")
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ConfirmRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Promotion reservation processed",
                reservationService.confirm(request, idempotencyKey, "SYSTEM")));
    }

    @PostMapping("/rollback")
    @Operation(summary = "Release an active promotion reservation after payment failure")
    public ResponseEntity<ApiResponse<ReservationResponse>> rollback(
            @Parameter(required = true, description = "Stable key for safe request retries")
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody RollbackRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Promotion reservation rolled back",
                reservationService.rollback(request, idempotencyKey, "SYSTEM")));
    }
}
