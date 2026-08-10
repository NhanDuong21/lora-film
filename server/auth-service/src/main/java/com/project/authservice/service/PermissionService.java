package com.project.authservice.service;

import com.project.authservice.dto.PermissionDto;
import java.util.List;

public interface PermissionService {
    List<PermissionDto> getAllPermissions();
    PermissionDto getPermissionById(Long id);
    PermissionDto createPermission(PermissionDto request);
    PermissionDto updatePermission(Long id, PermissionDto request);
    void deletePermission(Long id);
}
