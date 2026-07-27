package com.project.authservice.service;

import com.project.authservice.dto.RoleDto;
import java.util.List;

public interface RoleService {
    List<RoleDto> getAllRoles();
    RoleDto getRoleById(Long id);
    RoleDto createRole(RoleDto request);
    RoleDto updateRole(Long id, RoleDto request);
    void deleteRole(Long id);
}
