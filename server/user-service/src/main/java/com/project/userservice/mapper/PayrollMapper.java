package com.project.userservice.mapper;

import com.project.userservice.dto.response.PayrollDetailResponse;
import com.project.userservice.dto.response.PayrollResponse;
import com.project.userservice.entity.Payroll;
import com.project.userservice.entity.User;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Component
public class PayrollMapper {

    public PayrollResponse toResponse(Payroll payroll, User user, Map<Long, String> actorNames,
                                      boolean includeDetails) {
        return new PayrollResponse(payroll.getId(), payroll.getEmployee().getAccountId(),
                payroll.getEmployee().getEmployeeCode(), user == null ? null : user.getFullName(),
                payroll.getSalaryMonth(), payroll.getBasicSalary(), payroll.getAllowance(),
                payroll.getBonus(), payroll.getDeduction(), payroll.getTotalSalary(),
                payroll.getStatus(), payroll.getCreatedBy(), payroll.getApprovedBy(),
                payroll.getApprovedAt(), payroll.getPaidBy(), payroll.getPaidAt(),
                payroll.getPaymentReference(), payroll.getBankBatchReference(),
                payroll.getAccountingReference(), payroll.getReconciliationStatus(),
                payroll.getReconciledBy(), payroll.getReconciledAt(), payroll.getReconciliationNote(),
                payroll.getSourceType(), payroll.getSourceChecksum(), payroll.getScheduledMinutes(),
                payroll.getWorkedMinutes(), payroll.getPaidLeaveMinutes(), payroll.getOvertimeMinutes(),
                payroll.getCancelledBy(),
                payroll.getCancellationReason(), actorName(actorNames, payroll.getCreatedBy()),
                actorName(actorNames, payroll.getApprovedBy()), actorName(actorNames, payroll.getPaidBy()),
                actorName(actorNames, payroll.getReconciledBy()), actorName(actorNames, payroll.getCancelledBy()),
                payroll.getVersion(),
                includeDetails ? payroll.getDetails().stream()
                        .map(detail -> new PayrollDetailResponse(detail.getId(), detail.getType(),
                                detail.getDescription(), detail.getAmount()))
                        .toList() : Collections.emptyList());
    }

    private String actorName(Map<Long, String> actorNames, Long accountId) {
        return accountId == null ? null : actorNames.get(accountId);
    }
}
