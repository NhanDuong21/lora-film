package com.project.promotionservice.reservation.enums;

/**
 * Runtime state machine defined by the promotion reservation specification.
 */
public enum ReservationStatus {
    ACTIVE,
    CONFIRMED,
    RELEASED,
    EXPIRED
}
