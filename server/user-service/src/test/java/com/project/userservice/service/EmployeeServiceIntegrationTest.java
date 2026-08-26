package com.project.userservice.service;

import com.project.userservice.client.CinemaDirectoryClient;
import com.project.userservice.dto.request.EmployeeRequest;
import com.project.userservice.dto.request.EmploymentActionRequest;
import com.project.userservice.dto.response.EmployeeResponse;
import com.project.userservice.entity.Department;
import com.project.userservice.entity.Position;
import com.project.userservice.entity.User;
import com.project.userservice.enumtype.EmployeeStatus;
import com.project.userservice.enumtype.AccountType;
import com.project.userservice.enumtype.EmploymentActionType;
import com.project.userservice.enumtype.UserStatus;
import com.project.userservice.exception.BusinessException;
import com.project.userservice.repository.DepartmentRepository;
import com.project.userservice.repository.PositionRepository;
import com.project.userservice.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
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
class EmployeeServiceIntegrationTest {
    private static final String CINEMA_ID = "b1575c2d-9081-11f1-bf65-0ebab02bf6f5";

    @Autowired EmployeeService employeeService;
    @Autowired UserRepository userRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired PositionRepository positionRepository;
    @MockBean CinemaDirectoryClient cinemaDirectoryClient;

    private Department operations;
    private Department finance;
    private Position boxOffice;

    @BeforeEach
    void seedOrganization() {
        User user = new User();
        user.setAccountId(9101L);
        user.setFullName("Workforce Test User");
        user.setEmail("workforce@example.com");
        user.setAvatarUrl("/uploads/workforce-test-avatar.jpg");
        user.setAccountType(AccountType.WORKFORCE);
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);

        operations = department("TEST_OPS", "Test Operations");
        finance = department("TEST_FINANCE", "Test Finance Department");
        boxOffice = new Position();
        boxOffice.setCode("TEST_BOX_OFFICE");
        boxOffice.setTitle("Test Box Office");
        boxOffice.setDepartment(operations);
        boxOffice = positionRepository.save(boxOffice);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsPositionFromAnotherDepartment() {
        assertThatThrownBy(() -> employeeService.create(request(finance.getId(), boxOffice.getId())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không thuộc phòng ban");
    }

    @Test
    void rejectsCustomerAccountFromWorkforceOnboarding() {
        User customer = new User();
        customer.setAccountId(9102L);
        customer.setFullName("Customer Persona");
        customer.setEmail("customer-persona@example.com");
        userRepository.save(customer);

        EmployeeRequest request = new EmployeeRequest(9102L, operations.getId(), boxOffice.getId(),
                LocalDate.of(2026, 1, 1), new BigDecimal("12000000"), CINEMA_ID);

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("tài khoản khách hàng");
    }

    @Test
    void recordsAuditedEmploymentActionHistory() {
        EmployeeResponse created = employeeService.create(request(operations.getId(), boxOffice.getId()));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(88L, null, List.of()));

        EmployeeResponse cancelled = employeeService.applyAction(created.accountId(),
                new EmploymentActionRequest(EmploymentActionType.CANCEL_ONBOARDING, null, null, null,
                        LocalDate.now(), "Candidate declined the invitation", created.version()));

        assertThat(cancelled.status()).isEqualTo(EmployeeStatus.CANCELLED);
        var history = employeeService.actionHistory(created.accountId(), PageRequest.of(0, 10));
        assertThat(history.getTotalElements()).isEqualTo(1);
        assertThat(history.getContent().getFirst().type()).isEqualTo(EmploymentActionType.CANCEL_ONBOARDING);
        assertThat(history.getContent().getFirst().performedBy()).isEqualTo(88L);
    }

    @Test
    void cancelledOnboardingCanBeReopenedWithoutCreatingADuplicateEmployee() {
        EmployeeResponse created = employeeService.create(request(operations.getId(), boxOffice.getId()));
        assertThat(created.status()).isEqualTo(EmployeeStatus.ONBOARDING);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(88L, null, List.of()));

        EmployeeResponse cancelled = employeeService.applyAction(created.accountId(),
                new EmploymentActionRequest(EmploymentActionType.CANCEL_ONBOARDING, null, null, null,
                        LocalDate.now(), "Candidate is not ready to start", created.version()));
        EmployeeResponse reopened = employeeService.applyAction(created.accountId(),
                new EmploymentActionRequest(EmploymentActionType.REOPEN_ONBOARDING, null, null, null,
                        LocalDate.now(), "Candidate confirmed a new start date", cancelled.version()));

        assertThat(reopened.status()).isEqualTo(EmployeeStatus.ONBOARDING);
        assertThat(employeeService.actionHistory(created.accountId(), PageRequest.of(0, 10)).getTotalElements())
                .isEqualTo(2);
        assertThatThrownBy(() -> employeeService.create(request(operations.getId(), boxOffice.getId())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đã tồn tại");
    }

    @Test
    void alreadyActivatedWorkforceAccountCreatesAnActiveEmployeeProfile() {
        User activeWorkforce = new User();
        activeWorkforce.setAccountId(9103L);
        activeWorkforce.setFullName("Existing Active Workforce");
        activeWorkforce.setEmail("active-workforce@example.com");
        activeWorkforce.setAccountType(AccountType.WORKFORCE);
        activeWorkforce.setStatus(UserStatus.ACTIVE);
        userRepository.save(activeWorkforce);

        EmployeeResponse created = employeeService.create(new EmployeeRequest(
                9103L, operations.getId(), boxOffice.getId(), LocalDate.of(2026, 1, 1),
                new BigDecimal("12000000"), CINEMA_ID));

        assertThat(created.status()).isEqualTo(EmployeeStatus.ACTIVE);
    }

    @Test
    void createsAndReassignsEmployeeWithValidatedCinemaScope() {
        EmployeeResponse created = employeeService.create(request(operations.getId(), boxOffice.getId()));

        assertThat(created.cinemaPublicId()).isEqualTo(CINEMA_ID);
        assertThat(created.avatarUrl()).isEqualTo("/uploads/workforce-test-avatar.jpg");
        assertThat(created.status()).isEqualTo(EmployeeStatus.ONBOARDING);

        String nextCinema = "b1576780-9081-11f1-bf65-0ebab02bf6f5";
        EmployeeResponse reassigned = employeeService.assignCinema(created.accountId(), nextCinema.toUpperCase());

        assertThat(reassigned.cinemaPublicId()).isEqualTo(nextCinema);
        org.mockito.Mockito.verify(cinemaDirectoryClient).requireExisting(CINEMA_ID);
        org.mockito.Mockito.verify(cinemaDirectoryClient).requireExisting(nextCinema);
    }

    @Test
    void canExcludeTheCurrentlyAuthenticatedAccountFromTheAdminDirectory() {
        EmployeeResponse created = employeeService.create(request(operations.getId(), boxOffice.getId()));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(created.accountId(), null, List.of()));

        var visible = employeeService.search(null, null, null, null, null, true, PageRequest.of(0, 10));

        assertThat(visible.getContent()).noneMatch(item -> item.accountId().equals(created.accountId()));
    }

    private Department department(String code, String name) {
        Department value = new Department();
        value.setCode(code);
        value.setName(name);
        return departmentRepository.save(value);
    }

    private EmployeeRequest request(Long departmentId, Long positionId) {
        return new EmployeeRequest(9101L, departmentId, positionId,
                LocalDate.of(2026, 1, 1), new BigDecimal("12000000"), CINEMA_ID);
    }
}
