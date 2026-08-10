package com.lorafilm.movie.seat.dto;

public record BulkItemError(
        int index,
        String seatCode,
        String field,
        Object rejectedValue,
        String errorCode,
        String message
) {}
