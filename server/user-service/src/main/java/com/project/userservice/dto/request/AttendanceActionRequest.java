package com.project.userservice.dto.request;

import jakarta.validation.constraints.NotNull;

public record AttendanceActionRequest(@NotNull Long shiftId) {
}
