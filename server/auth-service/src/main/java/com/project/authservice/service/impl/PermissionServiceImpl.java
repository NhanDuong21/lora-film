package com.project.authservice.service.impl;

import com.project.authservice.dto.PermissionDto;
import com.project.authservice.entity.Permission;
import com.project.authservice.exception.DuplicateResourceException;
import com.project.authservice.exception.ResourceNotFoundException;
import com.project.authservice.repository.PermissionRepository;
import com.project.authservice.service.PermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final com.project.authservice.service.AuditLogService auditLogService;
    private final jakarta.servlet.http.HttpServletRequest request;

    @Override
    @Transactional(readOnly = true)
    public List<PermissionDto> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionDto getPermissionById(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found"));
        return mapToDto(permission);
    }

    @Override
    @Transactional
    public PermissionDto createPermission(PermissionDto request) {
        String code = normalizeCode(request.getCode());
        if (permissionRepository.existsByCode(code)) {
            throw new DuplicateResourceException("Permission code already exists: " + code);
        }
        Permission permission = Permission.builder()
                .code(code)
                .name(request.getName().trim())
                .module(normalizeModule(request.getModule(), code))
                .description(trimToNull(request.getDescription()))
                .build();
        permission = permissionRepository.save(permission);
        auditLogService.log(null, "CREATE_PERMISSION", this.request);
        return mapToDto(permission);
    }

    @Override
    @Transactional
    public PermissionDto updatePermission(Long id, PermissionDto request) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found"));
        permission.setName(request.getName().trim());
        permission.setModule(normalizeModule(request.getModule(), permission.getCode()));
        permission.setDescription(trimToNull(request.getDescription()));
        permission = permissionRepository.save(permission);
        auditLogService.log(null, "UPDATE_PERMISSION", this.request);
        return mapToDto(permission);
    }

    @Override
    @Transactional
    public void deletePermission(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found"));
        permissionRepository.delete(permission);
        auditLogService.log(null, "DELETE_PERMISSION", this.request);
    }

    private PermissionDto mapToDto(Permission permission) {
        return PermissionDto.builder()
                .id(permission.getId())
                .code(permission.getCode())
                .name(permission.getName())
                .module(permission.getModule())
                .description(permission.getDescription())
                .build();
    }

    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeModule(String module, String code) {
        if (module != null && !module.isBlank()) {
            return module.trim().toUpperCase(Locale.ROOT);
        }
        int separator = code.indexOf('_');
        return separator > 0 ? code.substring(0, separator) : "GENERAL";
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
    public PermissionServiceImpl(PermissionRepository permissionRepository, com.project.authservice.service.AuditLogService auditLogService, jakarta.servlet.http.HttpServletRequest request) {
        this.permissionRepository = permissionRepository;
        this.auditLogService = auditLogService;
        this.request = request;
    }
}
