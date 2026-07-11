package com.project.userservice.service.impl;

import com.project.userservice.dto.response.UserProfileResponse;
import com.project.userservice.entity.User;
import com.project.userservice.exception.BusinessException;
import com.project.userservice.repository.UserRepository;
import com.project.userservice.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserProfileResponse getUserProfile(Long accountId) {
        User user = userRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException("User profile not found", "USER_NOT_FOUND"));

        return mapToResponse(user);
    }

    private UserProfileResponse mapToResponse(User user) {
        UserProfileResponse response = new UserProfileResponse();
        response.setAccountId(user.getAccountId());
        response.setFullName(user.getFullName());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setGender(user.getGender());
        response.setBirthday(user.getBirthday());
        response.setCccdMasked(user.getCccdMasked());
        response.setProvinceName(user.getProvinceName());
        response.setBirthYear(user.getBirthYear());
        return response;
    }
}
