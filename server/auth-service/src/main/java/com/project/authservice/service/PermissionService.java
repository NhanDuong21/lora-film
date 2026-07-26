package com.project.authservice.service;

import com.project.authservice.dto.PermissionDto;
import java.util.List;

public interface PermissionService {
    List<PermissionDto> getAllPermissions();
    PermissionDto getPermissionById(Integer id);
    PermissionDto createPermission(PermissionDto request);
    PermissionDto updatePermission(Integer id, PermissionDto request);
    void deletePermission(Integer id);
}
