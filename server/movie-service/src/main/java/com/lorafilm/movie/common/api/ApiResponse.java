package com.lorafilm.movie.common.api;

public record ApiResponse<T>(boolean success, String message, T data) {

    // Thành công và có kèm dữ liệu trả về
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "Success", data);
    }

    // Thành công nhưng không cần trả về dữ liệu (ví dụ: Xóa thành công, Đăng xuất
    // thành công)
    public static <T> ApiResponse<T> ok(String message) {
        return new ApiResponse<>(true, message, null);
    }

    // Thất bại với một thông báo lỗi cụ thể (Dữ liệu data sẽ là null)
    public static <T> ApiResponse<T> fail(String errorMessage) {
        return new ApiResponse<>(false, errorMessage, null);
    }

    // Thất bại kèm theo mã lỗi hoặc dữ liệu lỗi chi tiết nếu cần (optional)
    public static <T> ApiResponse<T> fail(String errorMessage, T errorDetails) {
        return new ApiResponse<>(false, errorMessage, errorDetails);
    }
}