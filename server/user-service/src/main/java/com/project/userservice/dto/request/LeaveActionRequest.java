package com.project.userservice.dto.request;

import com.project.userservice.enumtype.LeaveActionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LeaveActionRequest(
        @NotNull LeaveActionType type,
        @Size(max = 500) String note,
        @NotNull Integer expectedVersion
) {
}
