package com.project.authservice.service;

import com.project.authservice.dto.RoleDto;
import java.util.List;

public interface RoleService {
    List<RoleDto> getAllRoles();
    RoleDto getRoleById(Integer id);
    RoleDto createRole(RoleDto request);
    RoleDto updateRole(Integer id, RoleDto request);
    void deleteRole(Integer id);
}
