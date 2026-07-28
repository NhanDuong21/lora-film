package com.project.authservice.controller;

import com.project.authservice.common.ApiResponse;
import com.project.authservice.dto.PermissionDto;
import com.project.authservice.service.PermissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
public class PermissionController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PermissionController.class);

    private final PermissionService permissionService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAnyAuthority('PERMISSION_VIEW', 'ROLE_CREATE', 'ROLE_UPDATE')")
    public ResponseEntity<ApiResponse<List<PermissionDto>>> getAllPermissions() {
        log.info("Get all permissions called");
        return ResponseEntity.ok(ApiResponse.success("Success", permissionService.getAllPermissions()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERMISSION_VIEW')")
    public ResponseEntity<ApiResponse<PermissionDto>> getPermissionById(@PathVariable Long id) {
        log.info("Get permission by id called: {}", id);
        return ResponseEntity.ok(ApiResponse.success("Success", permissionService.getPermissionById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERMISSION_CREATE')")
    public ResponseEntity<ApiResponse<PermissionDto>> createPermission(@Valid @RequestBody PermissionDto request) {
        log.info("Create permission called: {}", request.getCode());
        return ResponseEntity.ok(ApiResponse.success("Permission created successfully", permissionService.createPermission(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERMISSION_UPDATE')")
    public ResponseEntity<ApiResponse<PermissionDto>> updatePermission(@PathVariable Long id, @Valid @RequestBody PermissionDto request) {
        log.info("Update permission called: {}", id);
        return ResponseEntity.ok(ApiResponse.success("Permission updated successfully", permissionService.updatePermission(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERMISSION_DELETE')")
    public ResponseEntity<ApiResponse<Void>> deletePermission(@PathVariable Long id) {
        log.info("Delete permission called: {}", id);
        permissionService.deletePermission(id);
        return ResponseEntity.ok(ApiResponse.success("Permission deleted successfully", null));
    }
    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }
}
