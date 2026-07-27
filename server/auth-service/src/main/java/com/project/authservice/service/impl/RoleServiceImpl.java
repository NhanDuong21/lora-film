package com.project.authservice.service.impl;

import com.project.authservice.dto.PermissionDto;
import com.project.authservice.dto.RoleDto;
import com.project.authservice.entity.Permission;
import com.project.authservice.entity.Role;
import com.project.authservice.exception.ResourceNotFoundException;
import com.project.authservice.repository.PermissionRepository;
import com.project.authservice.repository.RoleRepository;
import com.project.authservice.service.RoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final com.project.authservice.service.AuditLogService auditLogService;
    private final jakarta.servlet.http.HttpServletRequest request;

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
        Role role = Role.builder()
                .roleName(request.getRoleName())
                .description(request.getDescription())
                .build();
                
        if (request.getPermissions() != null) {
            Set<Permission> permissions = new HashSet<>();
            for (PermissionDto p : request.getPermissions()) {
                Permission perm = permissionRepository.findById(p.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + p.getId()));
                permissions.add(perm);
            }
            role.setPermissions(permissions);
        }
        
        role = roleRepository.save(role);
        auditLogService.log(null, "CREATE_ROLE", this.request);
        return mapToDto(role);
    }

    @Override
    @Transactional
    public RoleDto updateRole(Long id, RoleDto request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
                
        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());
        
        if (request.getPermissions() != null) {
            Set<Permission> permissions = new HashSet<>();
            for (PermissionDto p : request.getPermissions()) {
                Permission perm = permissionRepository.findById(p.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + p.getId()));
                permissions.add(perm);
            }
            role.setPermissions(permissions);
        }
        
        role = roleRepository.save(role);
        auditLogService.log(null, "UPDATE_ROLE", this.request);
        return mapToDto(role);
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        roleRepository.delete(role);
        auditLogService.log(null, "DELETE_ROLE", this.request);
    }

    private RoleDto mapToDto(Role role) {
        Set<PermissionDto> permissions = role.getPermissions().stream()
                .map(p -> PermissionDto.builder()
                        .id(p.getId())
                        .permissionCode(p.getPermissionCode())
                        .description(p.getDescription())
                        .build())
                .collect(Collectors.toSet());
                
        return RoleDto.builder()
                .id(role.getId())
                .roleName(role.getRoleName())
                .description(role.getDescription())
                .permissions(permissions)
                .build();
    }
    public RoleServiceImpl(RoleRepository roleRepository, PermissionRepository permissionRepository, com.project.authservice.service.AuditLogService auditLogService, jakarta.servlet.http.HttpServletRequest request) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.auditLogService = auditLogService;
        this.request = request;
    }
}
