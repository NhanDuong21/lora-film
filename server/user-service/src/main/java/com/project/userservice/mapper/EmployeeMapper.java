package com.project.userservice.mapper;

import com.project.userservice.dto.response.EmployeeResponse;
import com.project.userservice.entity.Employee;
import com.project.userservice.entity.User;
import com.project.userservice.exception.BusinessException;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper {

    public EmployeeResponse toResponse(Employee employee, User user) {
        if (user == null) {
            throw new BusinessException("User not found", "USER_001");
        }
        return new EmployeeResponse(employee.getAccountId(), employee.getEmployeeCode(),
                user.getFullName(), user.getPhoneNumber(),
                employee.getDepartment().getId(), employee.getDepartment().getCode(),
                employee.getDepartment().getName(), employee.getPosition().getId(),
                employee.getPosition().getCode(), employee.getPosition().getTitle(),
                employee.getHireDate(), employee.getBaseSalary(), employee.getStatus());
    }
}
