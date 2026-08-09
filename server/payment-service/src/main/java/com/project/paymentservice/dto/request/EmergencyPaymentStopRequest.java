package com.project.paymentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record EmergencyPaymentStopRequest(
        @NotEmpty(message = "Danh sách đơn cần dừng thanh toán là bắt buộc")
        @Size(max = 200, message = "Mỗi lần chỉ được xử lý tối đa 200 đơn")
        List<@NotBlank String> bookingPublicIds,
        @NotBlank(message = "Lý do dừng thanh toán là bắt buộc")
        @Size(max = 500, message = "Lý do dừng thanh toán không được vượt quá 500 ký tự")
        String reason
) {
}
