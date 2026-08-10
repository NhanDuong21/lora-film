package com.project.authservice.util;

import java.util.Locale;

public final class SensitiveDataMasker {

    private SensitiveDataMasker() {
    }

    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "unknown";
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        int separator = normalized.indexOf('@');
        if (separator <= 0 || separator == normalized.length() - 1) {
            return normalized.charAt(0) + "***";
        }
        return normalized.charAt(0) + "***" + normalized.substring(separator);
    }
}
