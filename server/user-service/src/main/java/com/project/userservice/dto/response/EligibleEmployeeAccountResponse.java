package com.project.userservice.dto.response;

public record EligibleEmployeeAccountResponse(
        Long accountId,
        String fullName,
        String email,
        String phoneNumber
) {
}
