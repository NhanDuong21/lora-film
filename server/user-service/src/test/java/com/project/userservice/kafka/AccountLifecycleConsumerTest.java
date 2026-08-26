package com.project.userservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.userservice.entity.Employee;
import com.project.userservice.entity.User;
import com.project.userservice.enumtype.EmployeeStatus;
import com.project.userservice.enumtype.UserStatus;
import com.project.userservice.repository.CustomerProfileRepository;
import com.project.userservice.repository.EmployeeRepository;
import com.project.userservice.repository.UserRepository;
import com.project.userservice.service.UserAuditService;
import com.project.userservice.service.UserDomainEventService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountLifecycleConsumerTest {

    @Test
    void activatingInvitedAccountCompletesEmployeeOnboarding() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
        UserAuditService auditService = mock(UserAuditService.class);
        UserDomainEventService eventService = mock(UserDomainEventService.class);
        User user = new User();
        user.setAccountId(42L);
        user.setStatus(UserStatus.INACTIVE);
        Employee employee = new Employee();
        employee.setAccountId(42L);
        employee.setStatus(EmployeeStatus.ONBOARDING);
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(employeeRepository.findByAccountIdAndIsDeletedFalse(42L)).thenReturn(Optional.of(employee));

        AccountLifecycleConsumer consumer = new AccountLifecycleConsumer(
                new ObjectMapper(), userRepository, auditService, eventService,
                mock(CustomerProfileRepository.class), employeeRepository);
        consumer.consume("""
                {"eventType":"ACCOUNT_ACTIVATED","data":{"accountId":42,"status":"ACTIVE"}}
                """);

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
        verify(employeeRepository).save(employee);
        verify(auditService).log(eq("EMPLOYEE_ONBOARDING_COMPLETED"), eq("EMPLOYEE"), eq(42L),
                org.mockito.ArgumentMatchers.anyString());
    }
}
