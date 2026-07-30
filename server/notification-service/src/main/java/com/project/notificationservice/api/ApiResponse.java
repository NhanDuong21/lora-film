package com.project.notificationservice.api;

import java.time.Instant;

public record ApiResponse<T>(
        boolean success,
        String message,
        String errorCode,
        T data,
        Instant timestamp) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "OK", null, data, Instant.now());
    }

    public static <T> ApiResponse<T> accepted(T data) {
        return new ApiResponse<>(true, "Accepted", null, data, Instant.now());
    }

    public static ApiResponse<Void> error(String message, String errorCode) {
        return new ApiResponse<>(false, message, errorCode, null, Instant.now());
    }
}
