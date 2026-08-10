package com.lorafilm.movie.showtime.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateShowtimeBlockedSeatsRequest(
        @NotEmpty(message = "Vui lòng chọn ít nhất một ghế")
        @Size(max = 200, message = "Không thể xử lý quá 200 ghế trong một lần")
        List<@NotBlank(message = "Mã ghế không được để trống") String> seatPublicIds,

        @Size(max = 255, message = "Lý do không được dài quá 255 ký tự")
        String reason
) {
}
