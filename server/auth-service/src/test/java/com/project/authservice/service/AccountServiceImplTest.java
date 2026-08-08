package com.project.authservice.service;

import com.project.authservice.entity.Account;
import com.project.authservice.entity.Role;
import com.project.authservice.enums.AccountStatus;
import com.project.authservice.exception.BusinessException;
import com.project.authservice.repository.AccountRepository;
import com.project.authservice.repository.AccessProfileRepository;
import com.project.authservice.repository.RoleRepository;
import com.project.authservice.service.impl.AccountServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private CredentialRevocationService credentialRevocationService;
    @Mock
    private AuthOutboxService authOutboxService;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock
    private com.project.authservice.event.publisher.AuthAccountEventPublisher eventPublisher;
    @Mock
    private AccessProfileRepository accessProfileRepository;

    private AccountServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AccountServiceImpl(accountRepository, roleRepository, auditLogService,
                request, credentialRevocationService, authOutboxService, passwordEncoder, eventPublisher,
                accessProfileRepository);
    }

    @Test
    void activatingBlockedAccountPublishesUnlockedLifecycleEvent() {
        Account account = account(10L, role(1L, "CUSTOMER"), AccountStatus.LOCKED);
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        service.updateAccountStatus(10L, AccountStatus.ACTIVE);

        verify(auditLogService).log(10L, "UPDATE_ACCOUNT_STATUS", request);
    }

    @Test
    void roleChangePublishesRemovalAndAssignmentAndRevokesCredentials() {
        Role customer = role(1L, "CUSTOMER");
        Role manager = role(2L, "MANAGER");
        Account account = account(10L, customer, AccountStatus.ACTIVE);
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(accountRepository.save(account)).thenReturn(account);

        service.updateAccountRole(10L, 2L);

        verify(credentialRevocationService).revokeAll(10L);
        verify(authOutboxService).record(eq("ACCOUNT_ROLE_CHANGED"), eq(10L), any());
        verify(auditLogService).log(10L, "UPDATE_ACCOUNT_ROLE", request);
    }

    @Test
    void duplicateRoleAssignmentIsRejectedWithoutWritesOrEvents() {
        Role customer = role(1L, "CUSTOMER");
        Account account = account(10L, customer, AccountStatus.ACTIVE);
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> service.updateAccountRole(10L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already assigned");

        verify(accountRepository, never()).save(any());
        verify(credentialRevocationService, never()).revokeAll(any());
    }

    @Test
    void managerCinemaAssignmentsAreNormalizedAndCredentialsAreRevoked() {
        Role manager = Role.builder().id(2L).code("MANAGER").roleName("Cinema manager").build();
        Account account = account(10L, manager, AccountStatus.ACTIVE);
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        service.updateManagerCinemaAssignments(10L, Set.of(
                "B1575C2D-9081-11F1-BF65-0EBAB02BF6F5",
                "b1576780-9081-11f1-bf65-0ebab02bf6f5"));

        assertThat(account.getAssignedCinemaPublicIds()).containsExactly(
                "b1575c2d-9081-11f1-bf65-0ebab02bf6f5",
                "b1576780-9081-11f1-bf65-0ebab02bf6f5");
        verify(credentialRevocationService).revokeAll(10L);
        verify(authOutboxService).record(eq("MANAGER_CINEMA_ASSIGNMENTS_CHANGED"), eq(10L), any());
        verify(auditLogService).log(10L, "UPDATE_MANAGER_CINEMA_ASSIGNMENTS", request);
    }

    @Test
    void cinemaAssignmentsAreRejectedForNonManagerAccount() {
        Account account = account(10L,
                Role.builder().id(3L).code("EMPLOYEE").roleName("Employee").build(),
                AccountStatus.ACTIVE);
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.updateManagerCinemaAssignments(
                10L, Set.of("b1575c2d-9081-11f1-bf65-0ebab02bf6f5")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("MANAGER");

        verify(accountRepository, never()).save(any());
        verify(credentialRevocationService, never()).revokeAll(any());
    }

    @Test
    void employeeAccountUsesCanonicalEmployeeRole() {
        var employee = Role.builder().id(3L).code("EMPLOYEE").roleName("Employee").build();
        var createRequest = new com.project.authservice.dto.request.EmployeeAccountRequest();
        createRequest.setEmail("  new.staff@lorafilm.local ");
        createRequest.setPassword("Temporary@123");
        createRequest.setFullName("New Staff");
        createRequest.setAccessProfileId(7L);

        var accessProfile = new com.project.authservice.entity.AccessProfile();
        accessProfile.setId(7L);
        accessProfile.setCode("BOX_OFFICE");
        accessProfile.setName("Nhân viên bán vé");
        accessProfile.setActive(true);

        when(accountRepository.existsByEmail("new.staff@lorafilm.local")).thenReturn(false);
        when(roleRepository.findByCode("EMPLOYEE")).thenReturn(Optional.of(employee));
        when(accessProfileRepository.findById(7L)).thenReturn(Optional.of(accessProfile));
        when(passwordEncoder.encode("Temporary@123")).thenReturn("encoded");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account saved = invocation.getArgument(0);
            saved.setId(20L);
            return saved;
        });

        service.createEmployeeAccount(createRequest);

        verify(accountRepository).save(argThat(account ->
                        "new.staff@lorafilm.local".equals(account.getEmail())
                        && "EMPLOYEE".equals(account.getRole().getCode())
                        && "BOX_OFFICE".equals(account.getAccessProfile().getCode())
                        && account.getAccountStatus() == AccountStatus.ACTIVE));
        verify(eventPublisher).publishEmployeeAccountCreated(any(Account.class), eq("New Staff"));
    }

    private Account account(Long id, Role role, AccountStatus status) {
        return Account.builder()
                .id(id)
                .email("member@example.com")
                .passwordHash("hash")
                .roles(new java.util.HashSet<>(java.util.List.of(role)))
                .status(status)
                .build();
    }

    private Role role(Long id, String name) {
        return Role.builder().id(id).roleName(name).build();
    }
}
