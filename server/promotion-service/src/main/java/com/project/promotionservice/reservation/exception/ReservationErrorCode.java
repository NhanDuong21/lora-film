package com.project.promotionservice.reservation.exception;

public final class ReservationErrorCode {

    public static final String RESERVATION_NOT_FOUND = "RESERVATION_NOT_FOUND";
    public static final String RESERVATION_CONFLICT = "RESERVATION_CONFLICT";
    public static final String RESERVATION_EXPIRED = "RESERVATION_EXPIRED";
    public static final String RESERVATION_COMPLETED = "RESERVATION_COMPLETED";
    public static final String RESERVATION_IDEMPOTENCY_CONFLICT = "RESERVATION_IDEMPOTENCY_CONFLICT";
    public static final String RESERVATION_INVALID_STATE = "RESERVATION_INVALID_STATE";

    private ReservationErrorCode() {
    }
}
