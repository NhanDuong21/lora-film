package com.lorafilm.movie.movie.domain.enums;

public enum MovieStatus {
    DRAFT,
    UPCOMING,
    NOW_SHOWING,
    ENDED,
    INACTIVE;

    public static MovieStatus fromString(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }
        String normalized = status.trim().toLowerCase();
        if ("coming-soon".equals(normalized)) {
            return UPCOMING;
        }
        if ("now-showing".equals(normalized)) {
            return NOW_SHOWING;
        }
        try {
            return MovieStatus.valueOf(status.replace("-", "_").toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
