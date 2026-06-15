package com.project.userservice.service;

import com.project.userservice.dto.request.InternalUserCreateRequest;
import com.project.userservice.dto.response.UserProfileResponse;

public interface UserService {
    UserProfileResponse createUserProfile(InternalUserCreateRequest request);
    UserProfileResponse getUserProfile(Long accountId);
}
