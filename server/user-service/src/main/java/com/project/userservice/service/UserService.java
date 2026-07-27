package com.project.userservice.service;

import com.project.userservice.dto.response.UserProfileResponse;

import java.util.List;

public interface UserService {
    UserProfileResponse getUserProfile(Long accountId);

    List<UserProfileResponse> getUserProfiles(List<Long> accountIds);

    List<UserProfileResponse> searchUserProfiles(String query, int limit);
}
