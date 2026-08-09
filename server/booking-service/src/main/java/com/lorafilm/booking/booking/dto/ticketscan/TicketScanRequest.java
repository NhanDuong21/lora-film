package com.lorafilm.booking.booking.dto.ticketscan;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TicketScanRequest(
        @NotBlank(message = "Vui lòng quét hoặc nhập mã vé")
        @Size(max = 255, message = "Mã vé không được vượt quá 255 ký tự")
        String code,
        @Size(max = 80, message = "Tên cửa soát không được vượt quá 80 ký tự")
        String gateLabel) {
}
