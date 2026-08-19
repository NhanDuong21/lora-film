package com.project.promotionservice.promotion.enums;

/** Decision for one booking hold at the exact impact snapshot. */
public enum ForceReleaseDisposition {
    SAFE_TO_RELEASE,
    REPRICE_REQUIRED,
    BLOCKED_PAYMENT_IN_PROGRESS,
    BLOCKED_PAYMENT_SUCCESSFUL,
    BLOCKED_BOOKING_TERMINAL,
    BLOCKED_DEPENDENCY_UNAVAILABLE,
    BLOCKED_MISSING_BOOKING_REFERENCE
}
