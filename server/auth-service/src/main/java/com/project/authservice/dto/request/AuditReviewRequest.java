package com.project.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuditReviewRequest(
        @NotBlank(message = "Vui lòng chọn trạng thái rà soát") String status,
        @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự") String note) {
}
