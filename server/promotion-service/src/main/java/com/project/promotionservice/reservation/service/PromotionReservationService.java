package com.project.promotionservice.reservation.service;

import com.project.promotionservice.reservation.dto.request.ReservationRequests.ConfirmRequest;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.ReserveRequest;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.RollbackRequest;
import com.project.promotionservice.reservation.dto.response.ReservationResponse;

public interface PromotionReservationService {

    ReservationResponse reserve(ReserveRequest request, String idempotencyKey, String actor);

    ReservationResponse confirm(ConfirmRequest request, String idempotencyKey, String actor);

    ReservationResponse rollback(RollbackRequest request, String idempotencyKey, String actor);

    int expireDueReservations(String actor);
}
