package com.project.userservice.dto.response;

public record EmployeeDirectoryResponse(
        Long accountId,
        String employeeCode,
        String fullName,
        String avatarUrl,
        String positionCode,
        String positionName) {
}
