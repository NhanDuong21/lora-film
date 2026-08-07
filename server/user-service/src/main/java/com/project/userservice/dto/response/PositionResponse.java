package com.project.userservice.dto.response;

public record PositionResponse(
        Long id,
        String code,
        String name,
        String description,
        Long departmentId,
        String departmentCode,
        String departmentName,
        long activeEmployeeCount
) {
}
