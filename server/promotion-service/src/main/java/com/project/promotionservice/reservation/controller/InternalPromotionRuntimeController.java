package com.project.promotionservice.reservation.controller;

import com.project.promotionservice.common.response.ApiResponse;
import com.project.promotionservice.promotion.dto.request.PromotionCheckoutRequest;
import com.project.promotionservice.promotion.dto.response.PromotionCheckoutResponse;
import com.project.promotionservice.reservation.service.PromotionReservationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/runtime")
public class InternalPromotionRuntimeController {

    private final PromotionReservationService reservationService;

    public InternalPromotionRuntimeController(
            PromotionReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping({"/preview", "/validate"})
    @PreAuthorize("hasAnyRole('BOOKING_SERVICE', 'PAYMENT_SERVICE')")
    public ResponseEntity<ApiResponse<PromotionCheckoutResponse>> preview(
            @Valid @RequestBody PromotionCheckoutRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                reservationService.preview(request)));
    }
}
