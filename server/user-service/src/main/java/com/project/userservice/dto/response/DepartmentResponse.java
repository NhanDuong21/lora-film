package com.project.userservice.dto.response;

public record DepartmentResponse(
        Long id,
        String code,
        String name,
        String description,
        long activePositionCount,
        long activeEmployeeCount
) {
}
