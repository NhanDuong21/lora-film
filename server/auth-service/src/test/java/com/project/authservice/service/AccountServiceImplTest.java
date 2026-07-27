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
    private AuthOutboxService outboxService;
    @Mock
    private CredentialRevocationService revocationService;

    private AccountServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AccountServiceImpl(accountRepository, roleRepository, auditLogService,
                request, outboxService, revocationService);
    }

    @Test
    void activatingBlockedAccountPublishesUnlockedLifecycleEvent() {
        Account account = account(10L, role(1, "CUSTOMER"), AccountStatus.BLOCKED);
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        service.updateAccountStatus(10L, AccountStatus.ACTIVE);

        verify(outboxService).record(eq("ACCOUNT_UNLOCKED"), eq(10L),
                argThat(data -> data instanceof java.util.Map<?, ?> map
                        && AccountStatus.ACTIVE.name().equals(map.get("status"))));
        verify(auditLogService).log(10L, "UPDATE_ACCOUNT_STATUS", request);
    }

    @Test
    void roleChangePublishesRemovalAndAssignmentAndRevokesCredentials() {
        Role customer = role(1, "CUSTOMER");
        Role manager = role(2, "MANAGER");
        Account account = account(10L, customer, AccountStatus.ACTIVE);
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(roleRepository.findById(2)).thenReturn(Optional.of(manager));
        when(accountRepository.save(account)).thenReturn(account);

        service.updateAccountRole(10L, 2);

        verify(revocationService).revokeAll(10L);
        verify(outboxService).record(eq("ROLE_REMOVED"), eq("ACCOUNT"), eq(10L),
                argThat(data -> data instanceof java.util.Map<?, ?> map
                        && "CUSTOMER".equals(map.get("role"))));
        verify(outboxService).record(eq("ROLE_ASSIGNED"), eq("ACCOUNT"), eq(10L),
                argThat(data -> data instanceof java.util.Map<?, ?> map
                        && "MANAGER".equals(map.get("role"))));
    }

    @Test
    void duplicateRoleAssignmentIsRejectedWithoutWritesOrEvents() {
        Role customer = role(1, "CUSTOMER");
        Account account = account(10L, customer, AccountStatus.ACTIVE);
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(roleRepository.findById(1)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> service.updateAccountRole(10L, 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already assigned");

        verify(accountRepository, never()).save(any());
        verify(revocationService, never()).revokeAll(any());
        verify(outboxService, never()).record(any(), any(), any(), any());
    }

    private Account account(Long id, Role role, AccountStatus status) {
        return Account.builder()
                .id(id)
                .email("member@example.com")
                .passwordHash("hash")
                .role(role)
                .accountStatus(status)
                .build();
    }

    private Role role(Integer id, String name) {
        return Role.builder().id(id).roleName(name).build();
    }
}
