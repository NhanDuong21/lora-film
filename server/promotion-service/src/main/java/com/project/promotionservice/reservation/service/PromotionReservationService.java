package com.project.promotionservice.reservation.service;

import com.project.promotionservice.benefit.dto.response.BenefitResponses.ValidationResponse;
import com.project.promotionservice.benefit.enums.BenefitEnums.RedemptionType;
import com.project.promotionservice.common.response.PagedResponse;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.ConfirmRequest;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.RefreshRequest;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.ReserveRequest;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.RuntimeValidationRequest;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.TransitionRequest;
import com.project.promotionservice.reservation.dto.response.ReservationResponse;
import com.project.promotionservice.reservation.enums.ReservationStatus;

import java.time.Instant;

public interface PromotionReservationService {

    ReservationResponse reserve(ReserveRequest request, String idempotencyKey, String actor);

    ReservationResponse confirm(
            String reservationPublicId, ConfirmRequest request,
            String idempotencyKey, String actor);

    ReservationResponse release(
            String reservationPublicId, TransitionRequest request,
            String idempotencyKey, String actor);

    ReservationResponse cancel(
            String reservationPublicId, TransitionRequest request,
            String idempotencyKey, String actor);

    ReservationResponse refresh(
            String reservationPublicId, RefreshRequest request,
            String idempotencyKey, String actor);

    ReservationResponse getDetail(String reservationPublicId, String actor);

    ValidationResponse validateRuntime(RuntimeValidationRequest request);

    PagedResponse<ReservationResponse> history(
            RedemptionType type, ReservationStatus status,
            String userPublicId, String bookingPublicId, String orderPublicId,
            Instant from, Instant to, int page, int size);

    int expireDueReservations(String actor);
}
