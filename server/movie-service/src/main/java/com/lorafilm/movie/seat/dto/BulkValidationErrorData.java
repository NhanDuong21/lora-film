package com.lorafilm.movie.seat.dto;

import java.util.List;

public record BulkValidationErrorData(
        int totalItems,
        int validItems,
        int invalidItems,
        List<BulkItemError> errors
) {}
