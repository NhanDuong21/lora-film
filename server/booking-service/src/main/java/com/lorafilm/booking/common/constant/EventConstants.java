package com.lorafilm.booking.common.constant;

public final class EventConstants {

    private EventConstants() {
    }

    public static final String BOOKING_AGGREGATE_TYPE = "Booking";
    public static final String SEAT_RESERVATION_AGGREGATE_TYPE = "SeatReservation";
    public static final String EVENT_BOOKING_CREATED = "BookingCreated";
    public static final String EVENT_BOOKING_CONFIRMED = "BookingConfirmed";
    public static final String EVENT_BOOKING_CANCELLED = "BookingCancelled";
    public static final String EVENT_BOOKING_EXPIRED = "BookingExpired";
    public static final String EVENT_BOOKING_REFUNDED = "BookingRefunded";
}
