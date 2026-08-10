package com.lorafilm.booking.common.exception;

import org.springframework.http.HttpStatus;

public class SeatAlreadyReservedException extends BusinessException {

    public SeatAlreadyReservedException(String message) {
        super("SEAT_ALREADY_RESERVED", message, HttpStatus.CONFLICT);
    }
}
