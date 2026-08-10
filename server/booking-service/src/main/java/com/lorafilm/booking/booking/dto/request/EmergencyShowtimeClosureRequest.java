package com.lorafilm.booking.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmergencyShowtimeClosureRequest(
        @NotBlank(message = "Lý do đóng khẩn cấp là bắt buộc")
        @Size(max = 500, message = "Lý do đóng khẩn cấp không được vượt quá 500 ký tự")
        String reason
) {
}
