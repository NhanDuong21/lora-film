package com.project.userservice.service;

import com.project.userservice.dto.request.PayrollRequest;
import com.project.userservice.dto.response.PayrollResponse;
import com.project.userservice.entity.Department;
import com.project.userservice.entity.Employee;
import com.project.userservice.entity.Position;
import com.project.userservice.entity.User;
import com.project.userservice.enumtype.EmployeeStatus;
import com.project.userservice.enumtype.PayrollStatus;
import com.project.userservice.exception.BusinessException;
import com.project.userservice.repository.DepartmentRepository;
import com.project.userservice.repository.EmployeeRepository;
import com.project.userservice.repository.PositionRepository;
import com.project.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PayrollServiceIntegrationTest {
    @Autowired PayrollService payrollService;
    @Autowired UserRepository userRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired PositionRepository positionRepository;
    @Autowired EmployeeRepository employeeRepository;

    @BeforeEach
    void seedEmployee() {
        User user = new User();
        user.setAccountId(9001L);
        user.setFullName("Payroll Test User");
        user.setPhoneNumber("0900009001");
        userRepository.save(user);

        Department department = new Department();
        department.setCode("TEST_FIN");
        department.setName("Test Finance");
        department = departmentRepository.save(department);

        Position position = new Position();
        position.setCode("TEST_ACCOUNTANT");
        position.setTitle("Test Accountant");
        position = positionRepository.save(position);

        Employee employee = new Employee();
        employee.setAccountId(user.getAccountId());
        employee.setEmployeeCode("EMP00009001");
        employee.setDepartment(department);
        employee.setPosition(position);
        employee.setBaseSalary(new BigDecimal("10000000"));
        employee.setHireDate(LocalDate.of(2025, 1, 1));
        employee.setStatus(EmployeeStatus.ACTIVE);
        employeeRepository.save(employee);
    }

    @Test
    void calculatesSalaryAndEnforcesUniqueEmployeeMonth() {
        PayrollResponse created = payrollService.create(request("2026-07"));

        assertThat(created.totalSalary()).isEqualByComparingTo("11250000");
        assertThat(created.status()).isEqualTo(PayrollStatus.PENDING_APPROVAL);
        assertThatThrownBy(() -> payrollService.create(request("2026-07")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void approvedPayrollCannotBeEdited() {
        PayrollResponse created = payrollService.create(request("2026-06"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null, List.of()));

        PayrollResponse approved = payrollService.approve(created.id());

        assertThat(approved.status()).isEqualTo(PayrollStatus.APPROVED);
        assertThatThrownBy(() -> payrollService.update(created.id(), request("2026-06")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot be changed");
        SecurityContextHolder.clearContext();
    }

    private PayrollRequest request(String month) {
        return new PayrollRequest(9001L, month, new BigDecimal("10000000"),
                new BigDecimal("500000"), new BigDecimal("1000000"),
                new BigDecimal("250000"), List.of());
    }
}
