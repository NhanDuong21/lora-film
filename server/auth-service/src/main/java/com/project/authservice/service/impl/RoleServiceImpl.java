package com.project.authservice.service.impl;

import com.project.authservice.dto.PermissionDto;
import com.project.authservice.dto.RoleDto;
import com.project.authservice.entity.Permission;
import com.project.authservice.entity.Role;
import com.project.authservice.exception.BusinessException;
import com.project.authservice.exception.ResourceNotFoundException;
import com.project.authservice.repository.PermissionRepository;
import com.project.authservice.repository.RoleRepository;
import com.project.authservice.repository.AccountRepository;
import com.project.authservice.service.CredentialRevocationService;
import com.project.authservice.service.AuthOutboxService;
import com.project.authservice.service.RoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl implements RoleService {

    private static final Set<String> SYSTEM_ROLE_CODES = Set.of("ADMIN", "MANAGER", "EMPLOYEE", "CUSTOMER");

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final com.project.authservice.service.AuditLogService auditLogService;
    private final jakarta.servlet.http.HttpServletRequest request;
    private final AccountRepository accountRepository;
    private final CredentialRevocationService credentialRevocationService;
    private final AuthOutboxService authOutboxService;

    @Override
    @Transactional(readOnly = true)
    public List<RoleDto> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RoleDto getRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        return mapToDto(role);
    }

    @Override
    @Transactional
    public RoleDto createRole(RoleDto request) {
        throw new BusinessException("System roles are fixed and cannot be created from operations");
    }

    @Override
    @Transactional
    public RoleDto updateRole(Long id, RoleDto request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        if (!"EMPLOYEE".equals(role.getCode())) {
            throw new BusinessException("Only EMPLOYEE permissions can be changed from operations");
        }

        if (request.getPermissionIds() != null || request.getPermissions() != null) {
            role.setPermissions(resolvePermissions(request));
        }
        
        role = roleRepository.save(role);
        Role updatedRole = role;
        accountRepository.findAllByRolesId(updatedRole.getId()).forEach(account -> {
            credentialRevocationService.revokeAll(account.getId());
            authOutboxService.record("ACCOUNT_PERMISSIONS_CHANGED", account.getId(),
                    java.util.Map.of("accountId", account.getId(), "role", updatedRole.getCode()));
        });
        auditLogService.log(null, "UPDATE_ROLE", this.request);
        return mapToDto(role);
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        if (SYSTEM_ROLE_CODES.contains(role.getCode())) {
            throw new IllegalStateException("Built-in role cannot be deleted");
        }
        roleRepository.delete(role);
        auditLogService.log(null, "DELETE_ROLE", this.request);
    }

    private RoleDto mapToDto(Role role) {
        Set<PermissionDto> permissions = role.getPermissions().stream()
                .map(p -> PermissionDto.builder()
                        .id(p.getId())
                        .code(p.getCode())
                        .name(p.getName())
                        .module(p.getModule())
                        .description(p.getDescription())
                        .build())
                .collect(Collectors.toSet());
                
        return RoleDto.builder()
                .id(role.getId())
                .code(role.getCode())
                .name(role.getRoleName())
                .description(role.getDescription())
                .permissionIds(permissions.stream().map(PermissionDto::getId).collect(Collectors.toSet()))
                .permissions(permissions)
                .build();
    }

    private Set<Permission> resolvePermissions(RoleDto request) {
        Set<Long> ids = new HashSet<>();
        if (request.getPermissionIds() != null) {
            ids.addAll(request.getPermissionIds());
        }
        if (request.getPermissions() != null) {
            request.getPermissions().stream()
                    .map(PermissionDto::getId)
                    .filter(java.util.Objects::nonNull)
                    .forEach(ids::add);
        }
        Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(ids));
        if (permissions.size() != ids.size()) {
            throw new ResourceNotFoundException("One or more permissions were not found");
        }
        return permissions;
    }

    public RoleServiceImpl(RoleRepository roleRepository, PermissionRepository permissionRepository,
                           com.project.authservice.service.AuditLogService auditLogService,
                           jakarta.servlet.http.HttpServletRequest request,
                           AccountRepository accountRepository,
                           CredentialRevocationService credentialRevocationService,
                           AuthOutboxService authOutboxService) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.auditLogService = auditLogService;
        this.request = request;
        this.accountRepository = accountRepository;
        this.credentialRevocationService = credentialRevocationService;
        this.authOutboxService = authOutboxService;
    }
}
