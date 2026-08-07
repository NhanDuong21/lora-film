package com.project.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PiiRetentionRequest(
        @NotNull LocalDate retentionUntil,
        @NotBlank @Size(min = 5, max = 500) String reason
) {
}
