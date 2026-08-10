package com.lorafilm.booking.common.constant;

public final class ValidationConstants {

    private ValidationConstants() {
    }

    public static final String BOOKING_CODE_PATTERN = "^BK[0-9A-Z]{8,12}$";
    public static final String UUID_PATTERN = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$";
    public static final String SEAT_LABEL_PATTERN = "^[A-Z]{1,2}[0-9]{1,2}$";
    public static final int MAX_CANCEL_REASON_LENGTH = 255;
    public static final int MAX_NOTE_LENGTH = 500;
}
