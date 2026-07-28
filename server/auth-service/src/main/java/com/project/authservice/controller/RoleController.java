package com.project.authservice.controller;

import com.project.authservice.common.ApiResponse;
import com.project.authservice.dto.RoleDto;
import com.project.authservice.service.RoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RoleController.class);

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAnyAuthority('ROLE_VIEW', 'SYSTEM_CONFIGURATION')")
    public ResponseEntity<ApiResponse<List<RoleDto>>> getAllRoles() {
        log.info("Get all roles called");
        return ResponseEntity.ok(ApiResponse.success("Success", roleService.getAllRoles()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_VIEW')")
    public ResponseEntity<ApiResponse<RoleDto>> getRoleById(@PathVariable Long id) {
        log.info("Get role by id called: {}", id);
        return ResponseEntity.ok(ApiResponse.success("Success", roleService.getRoleById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_CREATE')")
    public ResponseEntity<ApiResponse<RoleDto>> createRole(@Valid @RequestBody RoleDto request) {
        log.info("Create role called: {}", request.getName());
        return ResponseEntity.ok(ApiResponse.success("Role created successfully", roleService.createRole(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_UPDATE')")
    public ResponseEntity<ApiResponse<RoleDto>> updateRole(@PathVariable Long id, @Valid @RequestBody RoleDto request) {
        log.info("Update role called: {}", id);
        return ResponseEntity.ok(ApiResponse.success("Role updated successfully", roleService.updateRole(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_DELETE')")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable Long id) {
        log.info("Delete role called: {}", id);
        roleService.deleteRole(id);
        return ResponseEntity.ok(ApiResponse.success("Role deleted successfully", null));
    }
    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }
}
