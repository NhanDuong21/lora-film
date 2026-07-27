package com.lorafilm.booking.common.exception;

import org.springframework.http.HttpStatus;

/**
 * A late SUCCESS must return a conflict while still committing the
 * reconciliation task written in the same transaction.
 */
public class LatePaymentSuccessException extends BusinessException {

    public LatePaymentSuccessException(String message) {
        super("BOOKING_NOT_PAYABLE", message, HttpStatus.CONFLICT);
    }
}
