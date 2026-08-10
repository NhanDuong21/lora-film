package com.project.userservice.dto.request;

import com.project.userservice.enumtype.CustomerAccessActionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CustomerAccessActionRequest(
        @NotNull CustomerAccessActionType type,
        @NotBlank @Size(min = 5, max = 500) String reason
) {
}
