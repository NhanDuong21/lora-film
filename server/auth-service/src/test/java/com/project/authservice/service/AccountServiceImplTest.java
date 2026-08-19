package com.project.authservice.service;

import com.project.authservice.entity.Account;
import com.project.authservice.entity.AccessProfile;
import com.project.authservice.entity.Permission;
import com.project.authservice.entity.Role;
import com.project.authservice.enums.AccountStatus;
import com.project.authservice.exception.BusinessException;
import com.project.authservice.repository.AccountRepository;
import com.project.authservice.repository.AccessProfileRepository;
import com.project.authservice.repository.RoleRepository;
import com.project.authservice.service.impl.AccountServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
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
    @Mock
    private com.project.authservice.repository.PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private com.project.authservice.client.NotificationClient notificationClient;

    private AccountServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AccountServiceImpl(accountRepository, roleRepository, auditLogService,
                request, credentialRevocationService, authOutboxService, passwordEncoder, eventPublisher,
                accessProfileRepository, passwordResetTokenRepository, notificationClient);
    }

    @AfterEach
    void clearSecurityContext() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void activatingBlockedAccountPublishesUnlockedLifecycleEvent() {
        Account account = account(10L, role(1L, "CUSTOMER"), AccountStatus.LOCKED);
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        service.updateAccountStatus(10L, AccountStatus.ACTIVE);

        verify(auditLogService).log(eq(10L), eq("UPDATE_ACCOUNT_STATUS"), eq(request),
                eq("10"), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void roleChangePublishesRemovalAndAssignmentAndRevokesCredentials() {
        Role customer = role(1L, "EMPLOYEE");
        Role manager = role(2L, "MANAGER");
        Account account = account(10L, customer, AccountStatus.ACTIVE);
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(accountRepository.save(account)).thenReturn(account);

        service.updateAccountRole(10L, 2L, null);

        verify(credentialRevocationService).revokeAll(10L);
        verify(authOutboxService).record(eq("ACCOUNT_ROLE_CHANGED"), eq(10L), any());
        verify(auditLogService).log(eq(10L), eq("UPDATE_ACCOUNT_ROLE"), eq(request),
                eq("10"), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void duplicateRoleAssignmentIsRejectedWithoutWritesOrEvents() {
        Role customer = role(1L, "CUSTOMER");
        Account account = account(10L, customer, AccountStatus.ACTIVE);
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> service.updateAccountRole(10L, 1L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đã thuộc vai trò");

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
        verify(auditLogService).log(eq(10L), eq("UPDATE_MANAGER_CINEMA_ASSIGNMENTS"), eq(request),
                eq("10"), org.mockito.ArgumentMatchers.anyString());
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
    void managerCanReceiveSeparateAuthorAccessProfileAndSessionsAreRevoked() {
        Role manager = role(2L, "MANAGER");
        Account account = account(10L, manager, AccountStatus.ACTIVE);
        AccessProfile profile = new AccessProfile();
        profile.setId(20L);
        profile.setCode("PROMOTION_LOCAL_AUTHOR");
        profile.setName("Promotion local author");
        profile.setActive(true);
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(accessProfileRepository.findById(20L)).thenReturn(Optional.of(profile));
        when(accountRepository.save(account)).thenReturn(account);

        var result = service.updateAccountAccessProfile(10L, 20L);

        assertThat(result.getAccessProfile().getCode())
                .isEqualTo("PROMOTION_LOCAL_AUTHOR");
        assertThat(account.getAccessProfile()).isSameAs(profile);
        verify(credentialRevocationService).revokeAll(10L);
        verify(authOutboxService).record(
                eq("ACCOUNT_ACCESS_PROFILE_CHANGED"), eq(10L), any());
    }

    @Test
    void managerSupplementalAccessProfileCanBeRemoved() {
        Role manager = role(2L, "MANAGER");
        Account account = account(10L, manager, AccountStatus.ACTIVE);
        AccessProfile profile = new AccessProfile();
        profile.setId(20L);
        profile.setCode("PROMOTION_LOCAL_AUTHOR");
        profile.setName("Promotion local author");
        profile.setActive(true);
        account.setAccessProfile(profile);
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        service.updateAccountAccessProfile(10L, null);

        assertThat(account.getAccessProfile()).isNull();
        verify(credentialRevocationService).revokeAll(10L);
        verify(authOutboxService).record(
                eq("ACCOUNT_ACCESS_PROFILE_CHANGED"), eq(10L), any());
    }

    @Test
    void employeeCannotReceiveManagerOnlyPromotionAuthorProfile() {
        Account account = account(10L, role(3L, "EMPLOYEE"), AccountStatus.ACTIVE);
        AccessProfile profile = new AccessProfile();
        profile.setId(20L);
        profile.setCode("PROMOTION_LOCAL_AUTHOR");
        profile.setName("Promotion local author");
        profile.setActive(true);
        Permission author = new Permission();
        author.setCode("PROMOTION_AUTHOR");
        profile.setPermissions(Set.of(author));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(accessProfileRepository.findById(20L)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> service.updateAccountAccessProfile(10L, 20L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("MANAGER");

        verify(accountRepository, never()).save(any());
        verify(credentialRevocationService, never()).revokeAll(any());
    }

    @Test
    void currentAdministratorCannotLockOwnAccount() {
        Account administrator = account(10L, role(1L, "ADMIN"), AccountStatus.ACTIVE);
        when(accountRepository.findById(10L)).thenReturn(Optional.of(administrator));
        authenticateAs(10L);

        assertThatThrownBy(() -> service.updateAccountStatus(10L, AccountStatus.LOCKED))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không thể tự khóa");

        verify(accountRepository, never()).save(any());
    }

    @Test
    void lastActiveAdministratorCannotBeLocked() {
        Account administrator = account(10L, role(1L, "ADMIN"), AccountStatus.ACTIVE);
        when(accountRepository.findById(10L)).thenReturn(Optional.of(administrator));
        when(accountRepository.countActiveAdministrators()).thenReturn(1L);
        authenticateAs(22L);

        assertThatThrownBy(() -> service.updateAccountStatus(10L, AccountStatus.LOCKED))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("quản trị viên hoạt động cuối cùng");
    }

    @Test
    void currentAdministratorCannotDemoteOwnAccount() {
        Account administrator = account(10L, role(1L, "ADMIN"), AccountStatus.ACTIVE);
        Role employee = role(3L, "EMPLOYEE");
        when(accountRepository.findById(10L)).thenReturn(Optional.of(administrator));
        when(roleRepository.findById(3L)).thenReturn(Optional.of(employee));
        authenticateAs(10L);

        assertThatThrownBy(() -> service.updateAccountRole(10L, 3L, "Điều chuyển công việc"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không thể tự thu hồi quyền quản trị");
    }

    @Test
    void elevatingToAdministratorRequiresReason() {
        Account employee = account(10L, role(3L, "EMPLOYEE"), AccountStatus.ACTIVE);
        Role administrator = role(1L, "ADMIN");
        when(accountRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(administrator));

        assertThatThrownBy(() -> service.updateAccountRole(10L, 1L, ""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("nhập lý do");
    }

    @Test
    void employeeAccountUsesCanonicalEmployeeRole() {
        var employee = Role.builder().id(3L).code("EMPLOYEE").roleName("Employee").build();
        var createRequest = new com.project.authservice.dto.request.EmployeeAccountRequest();
        createRequest.setEmail("  new.staff@lorafilm.local ");
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
        when(passwordEncoder.encode(org.mockito.ArgumentMatchers.anyString())).thenReturn("encoded");
        when(passwordResetTokenRepository.findByAccountIdAndIsUsedFalse(20L))
                .thenReturn(java.util.List.of());
        when(passwordResetTokenRepository.findFirstByAccountIdAndIsUsedFalseOrderByCreatedAtDesc(20L))
                .thenReturn(java.util.Optional.empty());
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
                        && account.getAccountStatus() == AccountStatus.INACTIVE
                        && !Boolean.TRUE.equals(account.getIsEnabled())));
        verify(eventPublisher).publishEmployeeAccountCreated(any(Account.class), eq("New Staff"));
        verify(passwordResetTokenRepository).save(argThat(token ->
                "EMPLOYEE_INVITATION".equals(token.getPurpose())
                        && token.getExpiredAt().isAfter(java.time.LocalDateTime.now().plusHours(47))));
        verify(notificationClient).sendEmployeeInvitation(eq(20L), eq("new.staff@lorafilm.local"),
                eq("New Staff"), org.mockito.ArgumentMatchers.anyString());
    }

    private Account account(Long id, Role role, AccountStatus status) {
        return Account.builder()
                .id(id)
                .email("member@example.com")
                .passwordHash("hash")
                .roles(new java.util.HashSet<>(java.util.List.of(role)))
                .status(status)
                .isEnabled(true)
                .isDeleted(false)
                .build();
    }

    private Role role(Long id, String code) {
        return Role.builder().id(id).code(code).roleName(code).build();
    }

    private void authenticateAs(Long accountId) {
        var authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "admin@lorafilm.local", accountId, java.util.List.of());
        org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(authentication);
    }
}
