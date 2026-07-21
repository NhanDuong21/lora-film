package com.lorafilm.booking.common.util;

import com.lorafilm.booking.common.exception.ValidationException;

import java.util.Collection;

public final class ValidationUtils {

    private ValidationUtils() {
    }

    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new ValidationException(message);
        }
    }

    public static void notEmpty(Collection<?> collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw new ValidationException(message);
        }
    }

    public static void notBlank(String string, String message) {
        if (string == null || string.isBlank()) {
            throw new ValidationException(message);
        }
    }
}
