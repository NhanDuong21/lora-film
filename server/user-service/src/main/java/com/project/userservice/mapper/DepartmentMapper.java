package com.project.userservice.mapper;

import com.project.userservice.dto.response.DepartmentResponse;
import com.project.userservice.entity.Department;
import org.springframework.stereotype.Component;

@Component
public class DepartmentMapper {

    public DepartmentResponse toResponse(Department department) {
        return toResponse(department, 0, 0);
    }

    public DepartmentResponse toResponse(Department department, long positionCount, long employeeCount) {
        return new DepartmentResponse(department.getId(), department.getCode(),
                department.getName(), department.getDescription(), positionCount, employeeCount);
    }
}
