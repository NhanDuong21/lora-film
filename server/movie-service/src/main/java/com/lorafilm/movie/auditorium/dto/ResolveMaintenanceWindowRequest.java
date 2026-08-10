package com.lorafilm.movie.auditorium.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolveMaintenanceWindowRequest(
        @AssertTrue(message = "Phải xác nhận phòng đã sẵn sàng hoạt động")
        boolean readinessConfirmed,
        @NotBlank(message = "Vui lòng nhập kết quả kiểm tra phòng")
        @Size(max = 500, message = "Kết quả kiểm tra không được vượt quá 500 ký tự")
        String resolutionNote
) {}
