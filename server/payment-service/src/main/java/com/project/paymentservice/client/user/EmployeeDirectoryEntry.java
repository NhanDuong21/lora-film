package com.project.paymentservice.client.user;

public record EmployeeDirectoryEntry(
        Long accountId,
        String employeeCode,
        String fullName,
        String avatarUrl,
        String positionCode,
        String positionName) {
}
