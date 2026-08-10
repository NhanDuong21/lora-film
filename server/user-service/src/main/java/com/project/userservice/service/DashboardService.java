package com.project.userservice.service;

import com.project.userservice.dto.response.DashboardSummaryResponse;
import com.project.userservice.enumtype.EmployeeStatus;
import com.project.userservice.enumtype.PayrollStatus;
import com.project.userservice.enumtype.UserStatus;
import com.project.userservice.repository.CustomerProfileRepository;
import com.project.userservice.repository.EmployeeRepository;
import com.project.userservice.repository.PayrollRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

@Service
public class DashboardService {
    private final CustomerProfileRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final PayrollRepository payrollRepository;

    public DashboardService(CustomerProfileRepository customerRepository, EmployeeRepository employeeRepository,
                            PayrollRepository payrollRepository) {
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.payrollRepository = payrollRepository;
    }

    @Cacheable(value = "userDashboard", key = "'summary'")
    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary() {
        return summary(null);
    }

    @Cacheable(value = "userDashboard", key = "'summary:' + (#excludeAccountId == null ? 'all' : #excludeAccountId)")
    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(Long excludeAccountId) {
        Map<String, Long> employeeStatuses = new java.util.LinkedHashMap<>();
        for (EmployeeStatus status : EmployeeStatus.values()) {
            employeeStatuses.put(status.name(), excludeAccountId == null
                    ? employeeRepository.countByStatusAndIsDeletedFalse(status)
                    : employeeRepository.countByStatusAndIsDeletedFalseAndAccountIdNot(status, excludeAccountId));
        }
        return new DashboardSummaryResponse(
                customerRepository.countActiveProfiles(),
                customerRepository.countByUserStatus(UserStatus.ACTIVE),
                customerRepository.countByUserStatus(UserStatus.BLOCKED),
                excludeAccountId == null
                        ? employeeRepository.countByIsDeletedFalse()
                        : employeeRepository.countByIsDeletedFalseAndAccountIdNot(excludeAccountId),
                employeeStatuses,
                payrollRepository.countByStatus(PayrollStatus.PENDING_APPROVAL),
                payrollRepository.countByStatus(PayrollStatus.APPROVED),
                payrollRepository.countByStatus(PayrollStatus.PAID),
                payrollRepository.sumTotalByStatuses(Arrays.asList(PayrollStatus.APPROVED, PayrollStatus.PAID)));
    }
}
