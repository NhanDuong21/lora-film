package com.project.userservice.service;

import com.project.userservice.dto.response.UserProfileResponse;
import com.project.userservice.dto.request.UpdateProfileRequest;

import java.util.List;

public interface UserService {
    UserProfileResponse getUserProfile(Long accountId);

    UserProfileResponse updateProfile(Long accountId, UpdateProfileRequest request);

    List<UserProfileResponse> getUserProfiles(List<Long> accountIds);

    List<UserProfileResponse> searchUserProfiles(String query, int limit);
}
