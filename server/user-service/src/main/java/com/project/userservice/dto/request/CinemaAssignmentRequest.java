package com.project.userservice.dto.request;

import jakarta.validation.constraints.Pattern;

public record CinemaAssignmentRequest(
        @Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "Mã rạp không hợp lệ")
        String cinemaPublicId
) {
}
