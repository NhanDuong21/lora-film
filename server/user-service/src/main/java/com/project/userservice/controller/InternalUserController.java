package com.project.userservice.controller;

import com.project.userservice.dto.request.InternalUserCreateRequest;
import com.project.userservice.dto.response.ApiResponse;
import com.project.userservice.dto.response.UserProfileResponse;
import com.project.userservice.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final UserService userService;

    public InternalUserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> createUserProfile(@RequestBody InternalUserCreateRequest request) {
        UserProfileResponse response = userService.createUserProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User profile created successfully", response));
    }
}
