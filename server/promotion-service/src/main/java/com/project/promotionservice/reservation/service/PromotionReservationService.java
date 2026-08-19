package com.project.promotionservice.reservation.service;

import com.project.promotionservice.common.response.PagedResponse;
import com.project.promotionservice.promotion.dto.request.PromotionCheckoutRequest;
import com.project.promotionservice.promotion.dto.response.PromotionCheckoutResponse;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.ConfirmRequest;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.CompensateRequest;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.RefreshRequest;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.ReserveRequest;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.TransitionRequest;
import com.project.promotionservice.reservation.dto.response.ReservationResponse;
import com.project.promotionservice.reservation.enums.ReservationStatus;

import java.time.Instant;
import java.util.Set;

public interface PromotionReservationService {

    PromotionCheckoutResponse preview(PromotionCheckoutRequest request);

    ReservationResponse reserve(ReserveRequest request, String idempotencyKey, String actor);

    ReservationResponse confirm(
            String reservationPublicId, ConfirmRequest request,
            String idempotencyKey, String actor);

    ReservationResponse release(
            String reservationPublicId, TransitionRequest request,
            String idempotencyKey, String actor);

    ReservationResponse reverseConfirmed(
            String reservationPublicId, CompensateRequest request,
            String idempotencyKey, String actor);

    ReservationResponse refresh(
            String reservationPublicId, RefreshRequest request,
            String idempotencyKey, String actor);

    ReservationResponse getDetail(String reservationPublicId, String actor);

    PagedResponse<ReservationResponse> history(
            ReservationStatus status,
            String userPublicId,
            String bookingPublicId,
            String orderPublicId,
            Instant from,
            Instant to,
            int page,
            int size);

    PagedResponse<ReservationResponse> history(
            ReservationStatus status,
            String userPublicId,
            String bookingPublicId,
            String orderPublicId,
            Instant from,
            Instant to,
            int page,
            int size,
            Set<String> allowedCampaignPublicIds);

    int expireDueReservations(String actor);
}
