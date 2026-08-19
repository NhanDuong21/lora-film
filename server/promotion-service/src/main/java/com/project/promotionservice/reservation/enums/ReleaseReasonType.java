package com.project.promotionservice.reservation.enums;

/** Stable operational taxonomy for releasing an unconfirmed hold. */
public enum ReleaseReasonType {
    PAYMENT_FAILED,
    PAYMENT_TIMEOUT,
    CUSTOMER_CANCELLED_BOOKING,
    STAFF_CANCELLED_BOOKING,
    BOOKING_EXPIRED,
    CAMPAIGN_PAUSED,
    CAMPAIGN_KILL_SWITCH,
    SYSTEM_COMPENSATION,
    /** Backfilled row whose historical business reason cannot be proven. */
    LEGACY_UNKNOWN
}
