package com.lorafilm.movie.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, String errorCode, String message, T data, List<String> errors) {

    // Thành công và có kèm dữ liệu trả về
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "SUCCESS", "Success", data, null);
    }

    // Thành công nhưng không cần trả về dữ liệu (ví dụ: Xóa thành công, Đăng xuất
    // thành công)
    public static <T> ApiResponse<T> ok(String message) {
        return new ApiResponse<>(true, "SUCCESS", message, null, null);
    }

    // Thất bại với một thông báo lỗi cụ thể (Dữ liệu data sẽ là null)
    public static <T> ApiResponse<T> fail(String errorCode, String errorMessage) {
        return new ApiResponse<>(false, errorCode, errorMessage, null, null);
    }

    // Thất bại kèm theo mã lỗi và dữ liệu chi tiết nếu cần
    public static <T> ApiResponse<T> fail(String errorCode, String errorMessage, T errorDetails) {
        return new ApiResponse<>(false, errorCode, errorMessage, errorDetails, null);
    }

    // Thất bại do validation
    public static <T> ApiResponse<T> validationFail(String errorCode, String errorMessage, List<String> errors) {
        return new ApiResponse<>(false, errorCode, errorMessage, null, errors);
    }
}