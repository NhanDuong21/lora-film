package com.project.userservice.controller;

import com.project.userservice.dto.response.ApiResponse;
import com.project.userservice.dto.response.UserProfileResponse;
import com.project.userservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.project.userservice.exception.ForbiddenException;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }
    @GetMapping("/{accountId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(@PathVariable Long accountId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Long tokenAccountId = (Long) authentication.getPrincipal();
        boolean isAdminOrStaff = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF"));

        if (!isAdminOrStaff && !accountId.equals(tokenAccountId)) {
            throw new ForbiddenException("You don't have permission to view this profile");
        }

        UserProfileResponse response = userService.getUserProfile(accountId);
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved successfully", response));
    }

    @GetMapping("/admin/batch")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> getUserProfiles(
            @RequestParam List<Long> accountIds) {
        return ResponseEntity.ok(ApiResponse.success(
                "User profiles retrieved successfully",
                userService.getUserProfiles(accountIds)));
    }

    @GetMapping("/admin/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> searchUserProfiles(
            @RequestParam String query,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(ApiResponse.success(
                "User profiles retrieved successfully",
                userService.searchUserProfiles(query, limit)));
    }
}
