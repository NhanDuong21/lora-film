package com.lorafilm.booking.booking.dto.ticketscan;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TicketGateHandoffRequest(
        @NotBlank(message = "Vui lòng nhập cửa hoặc khu vực soát vé")
        @Size(max = 80, message = "Tên cửa soát không được vượt quá 80 ký tự")
        String gateLabel,
        @Min(value = 0, message = "Số sự cố chưa xử lý không được âm")
        @Max(value = 999, message = "Số sự cố chưa xử lý không hợp lệ")
        int unresolvedIncidents,
        @Size(max = 1000, message = "Ghi chú bàn giao không được vượt quá 1000 ký tự")
        String note) {
}
