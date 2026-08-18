package com.project.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserAuditReviewRequest(
        @NotBlank(message = "Trạng thái rà soát là bắt buộc") String status,
        @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự") String note) {
}
