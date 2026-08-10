package com.lorafilm.booking.common.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class BookingNotFoundException extends BusinessException {

    public BookingNotFoundException(Long id) {
        super("BOOKING_NOT_FOUND", "Booking not found with ID: " + id, HttpStatus.NOT_FOUND);
    }

    public BookingNotFoundException(String bookingCode) {
        super("BOOKING_NOT_FOUND", "Booking not found with code: " + bookingCode, HttpStatus.NOT_FOUND);
    }

    public BookingNotFoundException(UUID publicId) {
        super("BOOKING_NOT_FOUND", "Booking not found with public ID: " + publicId, HttpStatus.NOT_FOUND);
    }
}
