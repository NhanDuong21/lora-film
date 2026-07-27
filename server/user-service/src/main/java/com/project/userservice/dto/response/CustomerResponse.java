package com.project.userservice.dto.response;

import com.project.userservice.enumtype.Gender;
import com.project.userservice.enumtype.UserStatus;

import java.time.LocalDate;

public record CustomerResponse(
        Long id,
        Long accountId,
        String customerCode,
        String fullName,
        String phoneNumber,
        Gender gender,
        LocalDate birthday,
        String avatarUrl,
        UserStatus status,
        LocalDate joinedAt,
        String note
) {
}
