package com.project.userservice.service.impl;

import com.project.userservice.dto.request.InternalUserCreateRequest;
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
    @Transactional
    public UserProfileResponse createUserProfile(InternalUserCreateRequest request) {
        if (userRepository.existsById(request.getAccountId())) {
            throw new BusinessException("User profile already exists for this account", "USER_PROFILE_ALREADY_EXISTS");
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BusinessException("Phone number already exists", "USER_PHONE_ALREADY_EXISTS");
        }

        if (userRepository.existsByCccd(request.getCccd())) {
            throw new BusinessException("CCCD already exists", "USER_CCCD_ALREADY_EXISTS");
        }

        User user = new User();
        user.setAccountId(request.getAccountId());
        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setCccd(request.getCccd());
        user.setCccdMasked(request.getCccdMasked());
        user.setProvinceCode(request.getProvinceCode());
        user.setProvinceName(request.getProvinceName());
        user.setBirthYear(request.getBirthYear());
        user.setGender(request.getGender());
        user.setBirthday(request.getBirthday());
        user.setCccdCheckNote(request.getCccdCheckNote());
        user.setVerifiedPhone(false);

        user = userRepository.save(user);

        return mapToResponse(user);
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
        response.setIsVerifiedPhone(user.getVerifiedPhone());
        return response;
    }
}
