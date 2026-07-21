package com.lorafilm.booking.audit.enums;

public enum OperationType {
    HOLD_SEATS,
    RELEASE_SEATS,
    CONVERT_RESERVATION,
    EXPIRE_RESERVATION,
    CREATE_BOOKING,
    CANCEL_BOOKING,
    PROCESS_PAYMENT,
    REFUND_PAYMENT
}
