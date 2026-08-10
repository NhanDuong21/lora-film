package com.project.authservice.service;

import com.project.authservice.dto.RoleDto;
import com.project.authservice.entity.Account;
import com.project.authservice.entity.Permission;
import com.project.authservice.entity.Role;
import com.project.authservice.exception.BusinessException;
import com.project.authservice.repository.AccountRepository;
import com.project.authservice.repository.PermissionRepository;
import com.project.authservice.repository.RoleRepository;
import com.project.authservice.service.impl.RoleServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {
    @Mock private RoleRepository roleRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private HttpServletRequest request;
    @Mock private AccountRepository accountRepository;
    @Mock private CredentialRevocationService credentialRevocationService;
    @Mock private AuthOutboxService authOutboxService;

    private RoleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RoleServiceImpl(roleRepository, permissionRepository, auditLogService, request,
                accountRepository, credentialRevocationService, authOutboxService);
    }

    @Test
    void creatingAdditionalOperationalRoleIsRejected() {
        assertThatThrownBy(() -> service.createRole(RoleDto.builder().code("CASHIER").name("Cashier").build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("fixed");

        verify(roleRepository, never()).save(any());
    }

    @Test
    void onlyEmployeePermissionsCanBeChanged() {
        Role manager = Role.builder().id(2L).code("MANAGER").roleName("Cinema manager").build();
        when(roleRepository.findById(2L)).thenReturn(Optional.of(manager));

        assertThatThrownBy(() -> service.updateRole(2L,
                RoleDto.builder().name("Cinema manager").permissionIds(Set.of()).build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("EMPLOYEE");

        verify(roleRepository, never()).save(any());
    }

    @Test
    void employeePermissionChangesRevokeEmployeeSessions() {
        Permission permission = Permission.builder().id(10L).code("EMPLOYEE_SCHEDULE_VIEW")
                .name("View own schedule").module("Employee Self Service").build();
        Role employee = Role.builder().id(5L).code("EMPLOYEE").roleName("Employee").permissions(Set.of()).build();
        Account account = Account.builder().id(20L).roles(new java.util.HashSet<>(Set.of(employee))).build();
        when(roleRepository.findById(5L)).thenReturn(Optional.of(employee));
        when(permissionRepository.findAllById(Set.of(10L))).thenReturn(List.of(permission));
        when(roleRepository.save(employee)).thenReturn(employee);
        when(accountRepository.findAllByRolesId(5L)).thenReturn(List.of(account));

        RoleDto result = service.updateRole(5L,
                RoleDto.builder().name("Employee").permissionIds(Set.of(10L)).build());

        assertThat(result.getPermissionIds()).containsExactly(10L);
        verify(credentialRevocationService).revokeAll(20L);
        verify(authOutboxService).record(eq("ACCOUNT_PERMISSIONS_CHANGED"), eq(20L), any());
        verify(auditLogService).log(null, "UPDATE_ROLE", request);
    }

    @Test
    void managerRoleCannotBeDeleted() {
        Role manager = Role.builder().id(2L).code("MANAGER").roleName("Cinema manager").build();
        when(roleRepository.findById(2L)).thenReturn(Optional.of(manager));

        assertThatThrownBy(() -> service.deleteRole(2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Built-in");

        verify(roleRepository, never()).delete(any());
    }
}
