package com.lorafilm.booking.common.exception;

import org.springframework.http.HttpStatus;

public class SeatReservationException extends BaseException {

    public SeatReservationException(String errorCode, String message, HttpStatus status) {
        super(errorCode, message, status);
    }

    public SeatReservationException(String errorCode, String message, HttpStatus status, Throwable cause) {
        super(errorCode, message, status, cause);
    }
}
