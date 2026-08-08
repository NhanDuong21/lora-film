package com.project.authservice.service;

import com.project.authservice.dto.AccessProfileDto;
import com.project.authservice.entity.AccessProfile;
import com.project.authservice.entity.Account;
import com.project.authservice.entity.Permission;
import com.project.authservice.repository.AccessProfileRepository;
import com.project.authservice.repository.AccountRepository;
import com.project.authservice.repository.PermissionRepository;
import com.project.authservice.service.impl.AccessProfileServiceImpl;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessProfileServiceImplTest {
    @Mock private AccessProfileRepository accessProfileRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private CredentialRevocationService credentialRevocationService;
    @Mock private AuthOutboxService authOutboxService;
    @Mock private AuditLogService auditLogService;
    @Mock private HttpServletRequest request;

    private AccessProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AccessProfileServiceImpl(accessProfileRepository, permissionRepository,
                accountRepository, credentialRevocationService, authOutboxService, auditLogService, request);
    }

    @Test
    void updatingProfilePermissionsRevokesAssignedEmployeeSessions() {
        Permission ticketScan = Permission.builder().id(13L).code("TICKET_SCAN")
                .name("Scan tickets").module("BOOKING").build();
        AccessProfile profile = new AccessProfile();
        profile.setId(2L);
        profile.setCode("TICKET_CHECKER");
        profile.setName("Nhân viên soát vé");
        profile.setActive(true);
        profile.setPermissions(Set.of());
        Account employee = Account.builder().id(5L).build();
        AccessProfileDto requestDto = new AccessProfileDto();
        requestDto.setPermissionIds(Set.of(13L));

        when(accessProfileRepository.findById(2L)).thenReturn(Optional.of(profile));
        when(permissionRepository.findAllById(Set.of(13L))).thenReturn(List.of(ticketScan));
        when(accessProfileRepository.save(profile)).thenReturn(profile);
        when(accountRepository.findAllByAccessProfileId(2L)).thenReturn(List.of(employee));
        when(accountRepository.countByAccessProfileId(2L)).thenReturn(1L);

        AccessProfileDto result = service.updatePermissions(2L, requestDto);

        assertThat(result.getPermissionIds()).containsExactly(13L);
        assertThat(result.getAssignedAccountCount()).isEqualTo(1L);
        verify(credentialRevocationService).revokeAll(5L);
        verify(authOutboxService).record(eq("ACCOUNT_PERMISSIONS_CHANGED"), eq(5L), any());
        verify(auditLogService).log(null, "UPDATE_ACCESS_PROFILE", request);
    }
}
