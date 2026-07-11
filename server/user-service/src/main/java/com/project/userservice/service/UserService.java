package com.project.userservice.service;

import com.project.userservice.dto.response.UserProfileResponse;

public interface UserService {
    UserProfileResponse getUserProfile(Long accountId);
}
