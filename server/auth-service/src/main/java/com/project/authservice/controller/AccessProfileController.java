package com.project.authservice.controller;

import com.project.authservice.common.ApiResponse;
import com.project.authservice.dto.AccessProfileDto;
import com.project.authservice.service.AccessProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/access-profiles")
public class AccessProfileController {
    private final AccessProfileService accessProfileService;

    public AccessProfileController(AccessProfileService accessProfileService) {
        this.accessProfileService = accessProfileService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAnyAuthority('ROLE_VIEW', 'PERMISSION_VIEW', 'SYSTEM_CONFIGURATION')")
    public ResponseEntity<ApiResponse<List<AccessProfileDto>>> getAllProfiles() {
        return ResponseEntity.ok(ApiResponse.success("Success", accessProfileService.getAllProfiles()));
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_UPDATE')")
    public ResponseEntity<ApiResponse<AccessProfileDto>> updatePermissions(
            @PathVariable Long id,
            @Valid @RequestBody AccessProfileDto request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Access profile permissions updated successfully",
                accessProfileService.updatePermissions(id, request)));
    }
}
