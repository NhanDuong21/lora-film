package com.project.authservice.service.impl;

import com.project.authservice.dto.AccessProfileDto;
import com.project.authservice.dto.PermissionDto;
import com.project.authservice.entity.AccessProfile;
import com.project.authservice.entity.Permission;
import com.project.authservice.exception.ResourceNotFoundException;
import com.project.authservice.repository.AccessProfileRepository;
import com.project.authservice.repository.AccountRepository;
import com.project.authservice.repository.PermissionRepository;
import com.project.authservice.service.AccessProfileService;
import com.project.authservice.service.AuditLogService;
import com.project.authservice.service.AuthOutboxService;
import com.project.authservice.service.CredentialRevocationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AccessProfileServiceImpl implements AccessProfileService {
    private final AccessProfileRepository accessProfileRepository;
    private final PermissionRepository permissionRepository;
    private final AccountRepository accountRepository;
    private final CredentialRevocationService credentialRevocationService;
    private final AuthOutboxService authOutboxService;
    private final AuditLogService auditLogService;
    private final HttpServletRequest request;

    public AccessProfileServiceImpl(AccessProfileRepository accessProfileRepository,
                                    PermissionRepository permissionRepository,
                                    AccountRepository accountRepository,
                                    CredentialRevocationService credentialRevocationService,
                                    AuthOutboxService authOutboxService,
                                    AuditLogService auditLogService,
                                    HttpServletRequest request) {
        this.accessProfileRepository = accessProfileRepository;
        this.permissionRepository = permissionRepository;
        this.accountRepository = accountRepository;
        this.credentialRevocationService = credentialRevocationService;
        this.authOutboxService = authOutboxService;
        this.auditLogService = auditLogService;
        this.request = request;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccessProfileDto> getAllProfiles() {
        return accessProfileRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional
    public AccessProfileDto updatePermissions(Long id, AccessProfileDto requestDto) {
        AccessProfile profile = accessProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Access profile not found"));
        Set<Long> permissionIds = requestDto.getPermissionIds() == null
                ? Set.of()
                : requestDto.getPermissionIds();
        Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(permissionIds));
        if (permissions.size() != permissionIds.size()) {
            throw new ResourceNotFoundException("One or more permissions were not found");
        }

        profile.setPermissions(permissions);
        AccessProfile saved = accessProfileRepository.save(profile);
        accountRepository.findAllByAccessProfileId(saved.getId()).forEach(account -> {
            credentialRevocationService.revokeAll(account.getId());
            authOutboxService.record("ACCOUNT_PERMISSIONS_CHANGED", account.getId(),
                    java.util.Map.of(
                            "accountId", account.getId(),
                            "role", "EMPLOYEE",
                            "accessProfile", saved.getCode()));
        });
        auditLogService.log(null, "UPDATE_ACCESS_PROFILE", request);
        return mapToDto(saved);
    }

    private AccessProfileDto mapToDto(AccessProfile profile) {
        Set<PermissionDto> permissions = profile.getPermissions().stream()
                .map(permission -> PermissionDto.builder()
                        .id(permission.getId())
                        .code(permission.getCode())
                        .name(permission.getName())
                        .module(permission.getModule())
                        .description(permission.getDescription())
                        .build())
                .collect(Collectors.toSet());
        AccessProfileDto dto = new AccessProfileDto();
        dto.setId(profile.getId());
        dto.setCode(profile.getCode());
        dto.setName(profile.getName());
        dto.setDescription(profile.getDescription());
        dto.setActive(profile.getActive());
        dto.setPermissionIds(permissions.stream().map(PermissionDto::getId).collect(Collectors.toSet()));
        dto.setPermissions(permissions);
        dto.setAssignedAccountCount(accountRepository.countByAccessProfileId(profile.getId()));
        return dto;
    }
}
