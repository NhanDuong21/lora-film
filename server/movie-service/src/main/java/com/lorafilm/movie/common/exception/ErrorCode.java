package com.lorafilm.movie.common.exception;

public enum ErrorCode {
    // --- General System ---
    INTERNAL_SERVER_ERROR("Internal server error", 500),
    VALIDATION_ERROR("Validation failed", 400),
    ACCESS_DENIED("Access denied", 403),
    RESOURCE_NOT_FOUND("Resource not found", 404),
    INTERNAL_TOKEN_INVALID("Internal token is invalid or expired", 401),

    // --- Movie Module ---
    MOVIE_NOT_FOUND("Movie not found", 404),
    MOVIE_VERSION_NOT_FOUND("Movie version not found", 404),
    MOVIE_VERSION_DUPLICATED("Movie version already exists", 400),

    // --- Cinema & Auditorium Module ---
    CINEMA_NOT_FOUND("Cinema not found", 404),
    AUDITORIUM_NOT_FOUND("Auditorium not found", 404),
    AUDITORIUM_NAME_DUPLICATED("Auditorium name already exists in this cinema", 400),
    CINEMA_CLOSURE_CONFLICT("Action conflicts with cinema closure schedule", 409),
    AUDITORIUM_MAINTENANCE_CONFLICT("Action conflicts with auditorium maintenance schedule", 409),

    // --- Seat Module ---
    SEAT_NOT_FOUND("Seat not found", 404),
    SEAT_TYPE_NOT_FOUND("Seat type not found", 404),
    SEAT_CODE_DUPLICATED("Seat code already exists in this auditorium", 400),
    SEAT_POSITION_DUPLICATED("Seat position (Row/Column) already occupied", 400),

    // --- Showtime Module ---
    SHOWTIME_NOT_FOUND("Showtime not found", 404),
    SHOWTIME_OVERLAP_CONFLICT("Showtime overlaps with an existing schedule", 409),
    SHOWTIME_PRICE_MISSING("Showtime price config is missing", 400),
    INVALID_SHOWTIME_STATUS_TRANSITION("Invalid showtime status transition", 400);

    private final String message;
    private final int httpStatus;

    ErrorCode(String message, int httpStatus) {
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String getMessage() {
        return message;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}