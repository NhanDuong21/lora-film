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
    MOVIE_PRIMARY_MEDIA_INVALID("Only POSTER and BANNER can be set as primary", 400),
    MOVIE_PRIMARY_POSTER_REQUIRED("Active primary poster is required to publish", 400),
    MOVIE_ACTIVE_VERSION_REQUIRED("Active version is required to publish", 400),
    MOVIE_PUBLISH_VALIDATION_FAILED("Movie publish validation failed", 400),
    MOVIE_MEDIA_NOT_FOUND("Movie media not found", 404),
    MOVIE_NOT_AVAILABLE_FOR_SCHEDULING("Movie is not available for showtime scheduling", 400),
    MOVIE_VERSION_NOT_ACTIVE("Movie version is not active", 400),
    MOVIE_VERSION_NOT_BELONG_TO_MOVIE("Movie version does not belong to the movie", 400),
    INVALID_MOVIE_DURATION("Invalid movie duration", 400),

    // --- Cinema & Auditorium Module ---
    CINEMA_NOT_FOUND("Cinema not found", 404),
    CINEMA_NOT_CONFIGURABLE("Cinema is not in a configurable state", 400),
    CINEMA_NOT_ACTIVE("Cinema is not active", 400),
    INVALID_CINEMA_TIMEZONE("Invalid cinema timezone", 400),
    AUDITORIUM_NOT_FOUND("Auditorium not found", 404),
    AUDITORIUM_NOT_ACTIVE("Auditorium is not active", 400),
    AUDITORIUM_NOT_BELONG_TO_CINEMA("Auditorium does not belong to the cinema", 400),
    AUDITORIUM_NAME_DUPLICATED("Auditorium name already exists in this cinema", 400),
    CINEMA_CLOSURE_CONFLICT("Action conflicts with cinema closure schedule", 409),
    AUDITORIUM_MAINTENANCE_CONFLICT("Action conflicts with auditorium maintenance schedule", 409),
    INVALID_AUDITORIUM_CAPACITY("Invalid auditorium capacity", 400),
    AUDITORIUM_CAPACITY_BELOW_CURRENT_SEAT_COUNT("Auditorium capacity cannot be lower than current active seats", 400),
    INVALID_CLEANING_BUFFER("Invalid cleaning buffer", 400),
    INVALID_AUDITORIUM_STATUS_TRANSITION("Invalid auditorium status transition", 400),
    AUDITORIUM_CANNOT_BE_DELETED_HAS_SHOWTIME_HISTORY("Auditorium cannot be deleted because it has showtime history", 409),
    AUDITORIUM_HAS_ACTIVE_SEATS("Auditorium cannot be deleted because it has active seats", 409),
    MAINTENANCE_WINDOW_NOT_FOUND("Maintenance window not found", 404),
    INVALID_MAINTENANCE_TIME_RANGE("Invalid maintenance time range", 400),
    MAINTENANCE_WINDOW_OVERLAPS("Maintenance window overlaps with an existing schedule", 409),
    MAINTENANCE_WINDOW_ALREADY_CANCELLED("Maintenance window is already cancelled", 400),
    MAINTENANCE_WINDOW_CANNOT_BE_CREATED_IN_PAST("Maintenance window cannot be created in the past", 400),

    // --- Seat Module ---
    SEAT_NOT_FOUND("Seat not found", 404),
    SEAT_TYPE_NOT_FOUND("Seat type not found", 404),
    SEAT_CODE_DUPLICATED("Seat code already exists in this auditorium", 400),
    SEAT_POSITION_DUPLICATED("Seat position (Row/Column) already occupied", 400),
    SEAT_TYPE_CODE_ALREADY_EXISTS("Seat type code already exists", 400),
    SEAT_TYPE_INACTIVE("Seat type is inactive", 400),
    SEAT_TYPE_IN_USE("Seat type is in use and cannot be deactivated", 409),
    INVALID_SEAT_TYPE_STATUS_TRANSITION("Invalid seat type status transition", 400),
    DUPLICATE_SEAT_CODE("Seat code already exists in database", 400),
    DUPLICATE_SEAT_POSITION("Seat position already exists in database", 400),
    DUPLICATE_SEAT_CODE_IN_REQUEST("Duplicate seat code found in request", 400),
    DUPLICATE_SEAT_POSITION_IN_REQUEST("Duplicate seat position found in request", 400),
    SEAT_CAPACITY_EXCEEDED("Seat capacity exceeded", 400),
    INVALID_SEAT_ROW("Invalid seat row", 400),
    INVALID_SEAT_NUMBER("Invalid seat number", 400),
    INVALID_SEAT_CODE("Invalid seat code", 400),
    INVALID_SEAT_POSITION("Invalid seat position", 400),
    INVALID_SEAT_STATUS_TRANSITION("Invalid seat status transition", 400),
    EMPTY_SEAT_BULK_REQUEST("Empty seat bulk request", 400),
    SEAT_BELONGS_TO_ANOTHER_AUDITORIUM("Seat belongs to another auditorium", 400),
    SEAT_INACTIVE("Seat is not active", 400),

    // --- Showtime Module ---
    SHOWTIME_NOT_FOUND("Showtime not found", 404),
    SHOWTIME_OVERLAP_CONFLICT("Showtime overlaps with an existing schedule", 409),
    SHOWTIME_PRICE_MISSING("Showtime price config is missing", 400),
    INVALID_SHOWTIME_STATUS_TRANSITION("Invalid showtime status transition", 400),
    
    AUDITORIUM_LAYOUT_NOT_EDITABLE("The seating arrangement cannot be changed while the auditorium is in operation", 409),
    BULK_SEAT_VALIDATION_ERROR("Invalid data for bulk seat creation", 400),

    // --- New Validation Error Codes ---
    SHOWTIME_OUTSIDE_RELEASE_WINDOW("Showtime outside release window", 409),
    SHOWTIME_OUTSIDE_OPERATING_HOURS("Showtime outside operating hours", 409),
    SHOWTIME_OVERLAPS_CINEMA_CLOSURE("Showtime overlaps cinema closure", 409),
    SHOWTIME_OVERLAPS_AUDITORIUM_MAINTENANCE("Showtime overlaps auditorium maintenance", 409),
    SHOWTIME_OVERLAP("Showtime overlap", 409);

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