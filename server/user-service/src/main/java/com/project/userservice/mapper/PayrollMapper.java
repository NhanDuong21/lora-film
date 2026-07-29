package com.project.userservice.mapper;

import com.project.userservice.dto.response.PayrollDetailResponse;
import com.project.userservice.dto.response.PayrollResponse;
import com.project.userservice.entity.Payroll;
import com.project.userservice.entity.User;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class PayrollMapper {

    public PayrollResponse toResponse(Payroll payroll, User user, boolean includeDetails) {
        return new PayrollResponse(payroll.getId(), payroll.getEmployee().getAccountId(),
                payroll.getEmployee().getEmployeeCode(), user == null ? null : user.getFullName(),
                payroll.getSalaryMonth(), payroll.getBasicSalary(), payroll.getAllowance(),
                payroll.getBonus(), payroll.getDeduction(), payroll.getTotalSalary(),
                payroll.getStatus(), payroll.getApprovedBy(), payroll.getApprovedAt(),
                payroll.getPaidAt(),
                includeDetails ? payroll.getDetails().stream()
                        .map(detail -> new PayrollDetailResponse(detail.getId(), detail.getType(),
                                detail.getDescription(), detail.getAmount()))
                        .toList() : Collections.emptyList());
    }
}
