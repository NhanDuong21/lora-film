package com.lorafilm.booking.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidBookingStatusException extends BusinessException {

    public InvalidBookingStatusException(String message) {
        super("INVALID_BOOKING_STATUS", message, HttpStatus.BAD_REQUEST);
    }
}
