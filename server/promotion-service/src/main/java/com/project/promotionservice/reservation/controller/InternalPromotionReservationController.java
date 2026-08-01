package com.project.promotionservice.reservation.controller;

import com.project.promotionservice.common.response.ApiResponse;
import com.project.promotionservice.common.response.ErrorResponse;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.reservation.enums.ReservationStatus;
import com.project.promotionservice.reservation.exception.ReservationErrorCode;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.ConfirmRequest;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.RefreshRequest;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.ReserveRequest;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.TransitionRequest;
import com.project.promotionservice.reservation.dto.response.ReservationResponse;
import com.project.promotionservice.reservation.idempotency.ReservationIdempotencyExecutor;
import com.project.promotionservice.reservation.service.PromotionReservationService;
import com.project.promotionservice.configuration.security.principal.InternalServicePrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.project.promotionservice.common.constant.ValidationConstants.UUID_PATTERN;

@RestController
@Validated
@RequestMapping("/internal/reservations")
@Tag(
        name = "Internal Reservation APIs",
        description = "Atomic promotion reservation lifecycle for Booking and Payment services")
@SecurityRequirement(name = "internalTokenAuth")
@ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400", description = "Malformed request or invalid promotion configuration",
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
    private final ReservationIdempotencyExecutor idempotencyExecutor;

    public InternalPromotionReservationController(
            PromotionReservationService reservationService,
            ReservationIdempotencyExecutor idempotencyExecutor) {
        this.reservationService = reservationService;
        this.idempotencyExecutor = idempotencyExecutor;
    }

    @PostMapping
    @PreAuthorize("hasRole('BOOKING_SERVICE')")
    @Operation(summary = "Evaluate and atomically reserve the best promotion set")
    public ResponseEntity<ApiResponse<ReservationResponse>> reserve(
            @Parameter(required = true, description = "Stable key for safe request retries")
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Parameter(hidden = true)
            @AuthenticationPrincipal InternalServicePrincipal principal,
            @Valid @RequestBody ReserveRequest request) {
        String actor = principal.getServiceName();
        ReservationResponse response = idempotencyExecutor.execute(
                actor,
                "POST /internal/reservations",
                idempotencyKey,
                null,
                request,
                HttpStatus.CREATED.value(),
                () -> reservationService.reserve(request, idempotencyKey, actor));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Promotion reserved successfully", response));
    }

    @PostMapping("/{reservationId}/confirm")
    @PreAuthorize("hasAnyRole('BOOKING_SERVICE', 'PAYMENT_SERVICE')")
    @Operation(summary = "Confirm payment and create the final redemption ledger entry")
    public ResponseEntity<ApiResponse<ReservationResponse>> confirm(
            @PathVariable
            @Pattern(regexp = UUID_PATTERN, message = "reservationId must be a valid UUID")
            String reservationId,
            @Parameter(required = true, description = "Stable key for safe request retries")
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Parameter(hidden = true)
            @AuthenticationPrincipal InternalServicePrincipal principal,
            @Valid @RequestBody ConfirmRequest request) {
        String actor = principal.getServiceName();
        ReservationResponse response = idempotencyExecutor.execute(
                actor,
                "POST /internal/reservations/{reservationId}/confirm",
                idempotencyKey,
                reservationId,
                request,
                HttpStatus.OK.value(),
                () -> reservationService.confirm(
                        reservationId, request, idempotencyKey, actor));
        requireConfirmableResult(response);
        return ResponseEntity.ok(ApiResponse.success("Promotion reservation confirmed", response));
    }

    @PostMapping("/{reservationId}/release")
    @PreAuthorize("hasAnyRole('BOOKING_SERVICE', 'PAYMENT_SERVICE')")
    @Operation(summary = "Release an active reservation after payment failure")
    public ResponseEntity<ApiResponse<ReservationResponse>> release(
            @PathVariable
            @Pattern(regexp = UUID_PATTERN, message = "reservationId must be a valid UUID")
            String reservationId,
            @Parameter(required = true, description = "Stable key for safe request retries")
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Parameter(hidden = true)
            @AuthenticationPrincipal InternalServicePrincipal principal,
            @Valid @RequestBody TransitionRequest request) {
        String actor = principal.getServiceName();
        return ResponseEntity.ok(ApiResponse.success(
                "Promotion reservation released",
                idempotencyExecutor.execute(
                        actor,
                        "POST /internal/reservations/{reservationId}/release",
                        idempotencyKey,
                        reservationId,
                        request,
                        HttpStatus.OK.value(),
                        () -> reservationService.release(
                                reservationId, request, idempotencyKey, actor))));
    }

    @PostMapping("/{reservationId}/cancel")
    @PreAuthorize("hasRole('BOOKING_SERVICE')")
    @Operation(summary = "Cancel an active reservation after booking cancellation")
    public ResponseEntity<ApiResponse<ReservationResponse>> cancel(
            @PathVariable
            @Pattern(regexp = UUID_PATTERN, message = "reservationId must be a valid UUID")
            String reservationId,
            @Parameter(required = true, description = "Stable key for safe request retries")
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Parameter(hidden = true)
            @AuthenticationPrincipal InternalServicePrincipal principal,
            @Valid @RequestBody TransitionRequest request) {
        String actor = principal.getServiceName();
        return ResponseEntity.ok(ApiResponse.success(
                "Promotion reservation cancelled",
                idempotencyExecutor.execute(
                        actor,
                        "POST /internal/reservations/{reservationId}/cancel",
                        idempotencyKey,
                        reservationId,
                        request,
                        HttpStatus.OK.value(),
                        () -> reservationService.release(
                                reservationId, request, idempotencyKey, actor))));
    }

    @PostMapping("/{reservationId}/refresh")
    @PreAuthorize("hasRole('BOOKING_SERVICE')")
    @Operation(summary = "Extend an active reservation to an absolute expiration time")
    public ResponseEntity<ApiResponse<ReservationResponse>> refresh(
            @PathVariable
            @Pattern(regexp = UUID_PATTERN, message = "reservationId must be a valid UUID")
            String reservationId,
            @Parameter(required = true, description = "Stable key for safe request retries")
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Parameter(hidden = true)
            @AuthenticationPrincipal InternalServicePrincipal principal,
            @Valid @RequestBody RefreshRequest request) {
        String actor = principal.getServiceName();
        ReservationResponse response = idempotencyExecutor.execute(
                actor,
                "POST /internal/reservations/{reservationId}/refresh",
                idempotencyKey,
                reservationId,
                request,
                HttpStatus.OK.value(),
                () -> reservationService.refresh(
                        reservationId, request, idempotencyKey, actor));
        requireConfirmableResult(response);
        return ResponseEntity.ok(ApiResponse.success(
                "Promotion reservation refreshed", response));
    }

    @GetMapping("/{reservationId}")
    @PreAuthorize("hasAnyRole('BOOKING_SERVICE', 'PAYMENT_SERVICE')")
    @Operation(summary = "Get reservation detail and self-heal an overdue active reservation")
    public ResponseEntity<ApiResponse<ReservationResponse>> getDetail(
            @PathVariable
            @Pattern(regexp = UUID_PATTERN, message = "reservationId must be a valid UUID")
            String reservationId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal InternalServicePrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                reservationService.getDetail(reservationId, principal.getServiceName())));
    }

    private void requireConfirmableResult(ReservationResponse response) {
        if (response.getStatus() == ReservationStatus.EXPIRED) {
            throw new BusinessException(
                    ReservationErrorCode.RESERVATION_EXPIRED,
                    "Reservation has expired",
                    HttpStatus.CONFLICT);
        }
    }
}
