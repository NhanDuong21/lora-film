package com.project.userservice.controller;

import com.project.userservice.dto.response.ApiResponse;
import com.project.userservice.dto.response.UserProfileResponse;
import com.project.userservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }
    @GetMapping("/{accountId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(@PathVariable Long accountId) {
        UserProfileResponse response = userService.getUserProfile(accountId);
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved successfully", response));
    }
}
