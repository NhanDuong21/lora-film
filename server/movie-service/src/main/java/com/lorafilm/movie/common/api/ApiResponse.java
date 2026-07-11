package com.lorafilm.movie.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, String message, String errorCode, T data) {

    // Thành công và có kèm dữ liệu trả về
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "Success", null, data);
    }

    // Thành công nhưng không cần trả về dữ liệu
    public static <T> ApiResponse<T> ok(String message) {
        return new ApiResponse<>(true, message, null, null);
    }

    // Thất bại với một thông báo lỗi cụ thể (Dữ liệu data sẽ là null)
    public static <T> ApiResponse<T> fail(String errorMessage) {
        return new ApiResponse<>(false, errorMessage, null, null);
    }

    // Thất bại với errorCode
    public static <T> ApiResponse<T> fail(String errorMessage, String errorCode) {
        return new ApiResponse<>(false, errorMessage, errorCode, null);
    }

    // Thất bại kèm theo mã lỗi và dữ liệu lỗi chi tiết
    public static <T> ApiResponse<T> fail(String errorMessage, String errorCode, T errorDetails) {
        return new ApiResponse<>(false, errorMessage, errorCode, errorDetails);
    }
}