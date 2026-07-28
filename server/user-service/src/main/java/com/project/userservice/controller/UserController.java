package com.project.userservice.controller;

import com.project.userservice.dto.response.ApiResponse;
import com.project.userservice.dto.response.UserProfileResponse;
import com.project.userservice.service.UserService;
import com.project.userservice.service.AvatarService;
import com.project.userservice.dto.request.UpdateProfileRequest;
import com.project.userservice.security.CurrentActor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.core.io.Resource;
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
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AvatarService avatarService;

    public UserController(UserService userService, AvatarService avatarService) {
        this.userService = userService;
        this.avatarService = avatarService;
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getOwnProfile() {
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved successfully",
                userService.getUserProfile(CurrentActor.accountId())));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateOwnProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success("User profile updated successfully",
                userService.updateProfile(CurrentActor.accountId(), request)));
    }

    @PostMapping(value = "/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadAvatar(
            @RequestPart("file") MultipartFile file) {
        String avatarUrl = avatarService.upload(CurrentActor.accountId(), file);
        return ResponseEntity.ok(ApiResponse.success("Avatar updated successfully",
                Map.of("avatarUrl", avatarUrl)));
    }

    @DeleteMapping("/profile/avatar")
    public ResponseEntity<ApiResponse<Void>> deleteAvatar() {
        avatarService.delete(CurrentActor.accountId());
        return ResponseEntity.ok(ApiResponse.success("Avatar deleted successfully", null));
    }

    @GetMapping("/profile/avatar/files/{fileName:.+}")
    public ResponseEntity<Resource> getAvatarFile(@PathVariable String fileName) {
        Resource resource = avatarService.load(fileName);
        MediaType mediaType = MediaTypeFactory.getMediaType(fileName)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource);
    }
    @GetMapping("/{accountId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(@PathVariable Long accountId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Long tokenAccountId = (Long) authentication.getPrincipal();
        boolean isAdminOrStaff = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_STAFF")
                        || a.getAuthority().equals("CUSTOMER_VIEW")
                        || a.getAuthority().equals("EMPLOYEE_VIEW"));

        if (!isAdminOrStaff && !accountId.equals(tokenAccountId)) {
            throw new ForbiddenException("You don't have permission to view this profile");
        }

        UserProfileResponse response = userService.getUserProfile(accountId);
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved successfully", response));
    }

    @GetMapping("/admin/batch")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')"
            + " or hasAnyAuthority('CUSTOMER_VIEW', 'EMPLOYEE_VIEW')")
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> getUserProfiles(
            @RequestParam List<Long> accountIds) {
        return ResponseEntity.ok(ApiResponse.success(
                "User profiles retrieved successfully",
                userService.getUserProfiles(accountIds)));
    }

    @GetMapping("/admin/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')"
            + " or hasAnyAuthority('CUSTOMER_VIEW', 'EMPLOYEE_VIEW')")
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> searchUserProfiles(
            @RequestParam String query,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(ApiResponse.success(
                "User profiles retrieved successfully",
                userService.searchUserProfiles(query, limit)));
    }
}
