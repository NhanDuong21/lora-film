package com.project.authservice.service;

import com.project.authservice.entity.Account;
import com.project.authservice.entity.Role;
import com.project.authservice.enums.AccountStatus;
import com.project.authservice.exception.BusinessException;
import com.project.authservice.repository.AccountRepository;
import com.project.authservice.repository.RoleRepository;
import com.project.authservice.service.impl.AccountServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

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

    private AccountServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AccountServiceImpl(accountRepository, roleRepository, auditLogService,
                request, credentialRevocationService, authOutboxService, passwordEncoder, eventPublisher);
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
