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
    GENRE_DUPLICATED("Genre already exists", 409),
    GENRE_IN_USE("Genre is in use by one or more active movies and cannot be deleted", 409),
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
    INVALID_MOVIE_STATUS_TRANSITION("Invalid movie status transition", 400),
    TMDB_IMPORT_INVALID_PAYLOAD("TMDB import payload is invalid", 400),
    TMDB_MOVIE_REVIEW_NOT_APPLICABLE("TMDB review is not applicable to this movie", 400),
    TMDB_PROVIDER_UNAVAILABLE("TMDB provider is unavailable", 502),
    TMDB_PROVIDER_RESPONSE_INVALID("TMDB provider response is invalid", 502),
    COMPANY_DUPLICATED("Production company name already exists", 409),

    // --- Cinema & Auditorium Module ---
    CINEMA_NOT_FOUND("Cinema not found", 404),
    CINEMA_NOT_CONFIGURABLE("Cinema is not in a configurable state", 400),
    CINEMA_NOT_ACTIVE("Cinema is not active", 400),
    CINEMA_MISSING_AUDITORIUM("Cannot activate cinema without any auditoriums", 400),
    CINEMA_MISSING_IMAGES("Cannot activate cinema without any images", 400),
    CINEMA_MISSING_OPERATING_HOURS("Cannot activate cinema without operating hours", 400),
    INVALID_CINEMA_TIMEZONE("Invalid cinema timezone", 400),
    INVALID_OPERATING_HOURS("Operating hour must be between 00:00 and 23:59", 400),
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
    AUDITORIUM_NOT_CONFIGURABLE("Auditorium is not in a configurable state", 400),
    AUDITORIUM_CANNOT_BE_DELETED_HAS_SHOWTIME_HISTORY("Auditorium cannot be deleted because it has showtime history", 409),
    AUDITORIUM_HAS_ACTIVE_SEATS("Auditorium cannot be deleted because it has active seats", 409),
    CLONE_AUDITORIUM_FAILED("Failed to clone auditorium layout", 400),
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
    SEAT_TYPE_INVALID("Seat type is invalid", 400),
    PRICE_INVALID("Price is invalid", 400),
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
    SEAT_BLOCKED_FOR_SHOWTIME("Seat is blocked for this showtime", 400),

    // --- Showtime Module ---
    SHOWTIME_NOT_FOUND("Showtime not found", 404),
    SHOWTIME_OVERLAP_CONFLICT("Showtime overlaps with an existing schedule", 409),
    SHOWTIME_PRICE_MISSING("Showtime price config is missing", 400),
    SHOWTIME_PRICE_NOT_EDITABLE("Cannot edit prices for this showtime status", 400),
    INVALID_SHOWTIME_STATUS_TRANSITION("Invalid showtime status transition", 400),
    
    AUDITORIUM_LAYOUT_NOT_EDITABLE("The seating arrangement cannot be changed while the auditorium is in operation", 409),
    BULK_SEAT_VALIDATION_ERROR("Invalid data for bulk seat creation", 400),

    // --- New Validation Error Codes ---
    SHOWTIME_OUTSIDE_RELEASE_WINDOW("Showtime outside release window", 409),
    SHOWTIME_OUTSIDE_OPERATING_HOURS("Showtime outside operating hours", 409),
    CINEMA_OPERATING_HOURS_NOT_CONFIGURED("Cinema operating hours are not configured for the selected day", 400),
    SHOWTIME_OVERLAPS_CINEMA_CLOSURE("Showtime overlaps cinema closure", 409),
    SHOWTIME_OVERLAPS_AUDITORIUM_MAINTENANCE("Showtime overlaps auditorium maintenance", 409),
    SHOWTIME_OVERLAP("Showtime overlap", 409),
    SHOWTIME_SCHEDULE_NOT_EDITABLE("Only draft showtimes can be updated", 409),
    SHOWTIME_SCHEDULING_CONFLICT("Showtime scheduling is being modified by another request", 409),
    
    // --- Showtime Lifecycle ---
    SHOWTIME_CANCELLATION_REASON_REQUIRED("Cancellation reason is required", 400),
    SHOWTIME_CANNOT_OPEN_AFTER_START("Cannot open showtime for booking after it has started", 400),
    SHOWTIME_CANNOT_FINISH_BEFORE_END("Cannot finish showtime before it ends", 400),
    SHOWTIME_BATCH_CANCELLATION_SAFETY_UNAVAILABLE("Batch cancellation safety cannot be verified", 409),
    
    // --- Auto Scheduling ---
    AUTO_SCHEDULE_PREVIEW_NOT_FOUND("Auto schedule preview not found", 404),
    AUTO_SCHEDULE_ITEM_NOT_FOUND("Auto schedule preview item not found", 404),
    AUTO_SCHEDULE_PREVIEW_NOT_EDITABLE("Auto schedule preview is not editable", 409),
    AUTO_SCHEDULE_PREVIEW_NOT_APPLICABLE("Auto schedule preview cannot be applied", 409),
    AUTO_SCHEDULE_PREVIEW_EXPIRED("Auto schedule preview has expired", 409),
    AUTO_SCHEDULE_PREVIEW_ALREADY_APPLIED("Auto schedule preview has already been applied", 409),
    AUTO_SCHEDULE_PREVIEW_APPLY_IN_PROGRESS("Auto schedule preview is currently being applied", 409),
    AUTO_SCHEDULE_PREVIEW_VERSION_CONFLICT("Auto schedule preview was modified by another request", 409),
    IDEMPOTENCY_KEY_REUSED("Idempotency key was reused with a different request", 409),
    AUTO_SCHEDULE_ITEM_NOT_BELONG_TO_PREVIEW("Preview item does not belong to the preview", 400),
    AUTO_SCHEDULE_REJECTED_ITEM_CANNOT_BE_SELECTED("Rejected preview item cannot be selected", 400),
    AUTO_SCHEDULE_DUPLICATE_ITEM_SELECTION("Duplicate preview item found in selection request", 400),
    AUTO_SCHEDULE_SELECTION_OVERLAP("Selected preview items overlap in an auditorium", 409),
    AUTO_SCHEDULE_INVALID_ITEM_SELECTION("Preview item cannot be selected in its current state", 409),
    
    AUTO_SCHEDULE_INVALID_DATE_RANGE("Invalid auto schedule date range", 400),
    AUTO_SCHEDULE_DATE_RANGE_TOO_LARGE("Auto schedule date range exceeds maximum allowed days", 400),
    AUTO_SCHEDULE_EMPTY_MOVIE_VERSIONS("Auto schedule movie versions cannot be empty after normalization", 400),
    AUTO_SCHEDULE_EMPTY_AUDITORIUMS("Auto schedule auditoriums cannot be empty after normalization", 400),
    AUTO_SCHEDULE_UNSUPPORTED_SLOT_GRANULARITY("Unsupported auto schedule slot granularity", 400),
    AUTO_SCHEDULE_INVALID_PREVIEW_TTL("Invalid auto schedule preview TTL", 400),
    AUTO_SCHEDULE_TOO_MANY_CANDIDATES("Too many auto schedule candidates generated", 422),
    AUTO_SCHEDULE_GENERATION_FAILED("Auto schedule generation failed", 500),
    AUTO_SCHEDULE_SELECTION_INVARIANT_VIOLATION("Optimized auto schedule selection is inconsistent", 500),
    
    AUTO_SCHEDULE_NO_SELECTED_ITEMS("No selected items to apply", 400),
    AUTO_SCHEDULE_SELECTED_ITEMS_OVERLAP("Selected items overlap with each other", 409),
    AUTO_SCHEDULE_APPLY_REVALIDATION_FAILED("One or more selected schedule candidates are no longer valid", 409),
    AUTO_SCHEDULE_CANDIDATE_CHANGED("Candidate data has changed and is no longer consistent", 409),
    AUTO_SCHEDULE_PREVIEW_DATA_INCONSISTENT("Preview data is inconsistent", 409),
    AUTO_SCHEDULE_APPLY_FAILED("Auto schedule apply failed", 500),

    CURRENT_USER_NOT_AVAILABLE("Current user not available", 401),

    // --- Cinema Deletion ---
    CINEMA_CANNOT_BE_DELETED_HAS_AUDITORIUMS("Cinema cannot be deleted because it has auditoriums", 409),
    CINEMA_CANNOT_BE_DELETED_HAS_SHOWTIME_HISTORY("Cinema cannot be deleted because it has showtime history", 409),

    // --- Location Integration ---
    LOCATION_QUERY_INVALID("Location query is invalid", 400),
    LOCATION_API_RATE_LIMITED("Location API rate limit exceeded", 429),
    LOCATION_API_TIMEOUT("Location API request timed out", 504),
    LOCATION_API_UNAVAILABLE("Location API is unavailable", 502),
    LOCATION_API_RESPONSE_INVALID("Location API response is invalid", 502),
    LOCATION_API_NOT_CONFIGURED("Location API is not configured", 500);

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
