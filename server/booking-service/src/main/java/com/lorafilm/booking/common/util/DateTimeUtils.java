package com.lorafilm.booking.common.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtils {

    public static final ZoneId UTC_ZONE = ZoneId.of("UTC");
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT.withZone(UTC_ZONE);

    private DateTimeUtils() {
    }

    public static Instant now() {
        return Instant.now();
    }

    public static String formatIso(Instant instant) {
        return instant != null ? ISO_FORMATTER.format(instant) : null;
    }
}
