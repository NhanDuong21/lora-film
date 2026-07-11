package com.lorafilm.movie.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, String errorCode, String message, T data, List<String> errors) {

    // Success with data payload
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "SUCCESS", "Success", data, java.util.Collections.emptyList());
    }

    // Success without data payload
    public static <T> ApiResponse<T> ok(String message) {
        return new ApiResponse<>(true, "SUCCESS", message, null, java.util.Collections.emptyList());
    }

    // Failure with specific error message
    public static <T> ApiResponse<T> fail(String errorCode, String errorMessage) {
        return new ApiResponse<>(false, errorCode, errorMessage, null, null);
    }

    // Failure with error code and detailed error data if needed
    public static <T> ApiResponse<T> fail(String errorCode, String errorMessage, T errorDetails) {
        return new ApiResponse<>(false, errorCode, errorMessage, errorDetails, null);
    }

    // Failure due to validation errors
    public static <T> ApiResponse<T> validationFail(String errorCode, String errorMessage, List<String> errors) {
        return new ApiResponse<>(false, errorCode, errorMessage, null, errors);
    }
}
