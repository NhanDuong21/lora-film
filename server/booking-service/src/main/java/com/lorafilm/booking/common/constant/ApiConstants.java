package com.lorafilm.booking.common.constant;

public final class ApiConstants {

    private ApiConstants() {
    }

    public static final String API_PREFIX = "/api";
    public static final String BOOKING_ENDPOINT = API_PREFIX + "/bookings";
    public static final String SEAT_RESERVATION_ENDPOINT = API_PREFIX + "/seat-reservations";
    public static final String HEADER_CORRELATION_ID = "X-Correlation-ID";
    public static final String HEADER_IDEMPOTENCY_KEY = "X-Idempotency-Key";
}
