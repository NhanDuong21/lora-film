package com.project.authservice.service.impl;

import com.project.authservice.dto.PermissionDto;
import com.project.authservice.entity.Permission;
import com.project.authservice.exception.ResourceNotFoundException;
import com.project.authservice.repository.PermissionRepository;
import com.project.authservice.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
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
    public PermissionDto getPermissionById(Integer id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found"));
        return mapToDto(permission);
    }

    @Override
    @Transactional
    public PermissionDto createPermission(PermissionDto request) {
        Permission permission = Permission.builder()
                .permissionCode(request.getPermissionCode())
                .description(request.getDescription())
                .build();
        permission = permissionRepository.save(permission);
        auditLogService.log(null, "CREATE_PERMISSION", this.request);
        return mapToDto(permission);
    }

    @Override
    @Transactional
    public PermissionDto updatePermission(Integer id, PermissionDto request) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found"));
        permission.setPermissionCode(request.getPermissionCode());
        permission.setDescription(request.getDescription());
        permission = permissionRepository.save(permission);
        auditLogService.log(null, "UPDATE_PERMISSION", this.request);
        return mapToDto(permission);
    }

    @Override
    @Transactional
    public void deletePermission(Integer id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found"));
        permissionRepository.delete(permission);
        auditLogService.log(null, "DELETE_PERMISSION", this.request);
    }

    private PermissionDto mapToDto(Permission permission) {
        return PermissionDto.builder()
                .id(permission.getId())
                .permissionCode(permission.getPermissionCode())
                .description(permission.getDescription())
                .build();
    }
}
