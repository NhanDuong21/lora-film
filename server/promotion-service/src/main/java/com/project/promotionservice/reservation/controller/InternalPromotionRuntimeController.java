package com.project.promotionservice.reservation.controller;

import com.project.promotionservice.benefit.dto.response.BenefitResponses.ValidationResponse;
import com.project.promotionservice.common.response.ApiResponse;
import com.project.promotionservice.common.response.ErrorResponse;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.RuntimeValidationRequest;
import com.project.promotionservice.reservation.service.PromotionReservationService;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/runtime")
@Tag(
        name = "Promotion Runtime APIs",
        description = "Advisory validation before the authoritative reservation operation")
@SecurityRequirement(name = "internalTokenAuth")
@ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400", description = "Promotion is not eligible",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404", description = "Benefit or campaign was not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409", description = "Benefit, quota or budget is already reserved",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class InternalPromotionRuntimeController {

    private final PromotionReservationService reservationService;

    public InternalPromotionRuntimeController(PromotionReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/validate")
    @PreAuthorize("hasAnyRole('BOOKING_SERVICE', 'PAYMENT_SERVICE')")
    @Operation(
            summary = "Validate current promotion availability and calculate pricing",
            description = "This operation does not hold capacity. POST /internal/reservations "
                    + "is the authoritative checkout operation.")
    public ResponseEntity<ApiResponse<ValidationResponse>> validate(
            @Valid @RequestBody RuntimeValidationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                reservationService.validateRuntime(request)));
    }
}
