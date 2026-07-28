package com.project.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DepartmentRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_]{2,20}$") String code,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 255) String description
) {
}
