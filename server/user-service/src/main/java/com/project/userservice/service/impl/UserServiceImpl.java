package com.project.userservice.service.impl;

import com.project.userservice.dto.request.InternalUserCreateRequest;
import com.project.userservice.dto.response.UserProfileResponse;
import com.project.userservice.entity.User;
import com.project.userservice.exception.BusinessException;
import com.project.userservice.repository.UserRepository;
import com.project.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

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

        User user = User.builder()
                .accountId(request.getAccountId())
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .cccd(request.getCccd())
                .cccdMasked(request.getCccdMasked())
                .provinceCode(request.getProvinceCode())
                .provinceName(request.getProvinceName())
                .birthYear(request.getBirthYear())
                .gender(request.getGender())
                .birthday(request.getBirthday())
                .cccdCheckNote(request.getCccdCheckNote())
                .isVerifiedPhone(false)
                .build();

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
        return UserProfileResponse.builder()
                .accountId(user.getAccountId())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .gender(user.getGender())
                .birthday(user.getBirthday())
                .cccdMasked(user.getCccdMasked())
                .provinceName(user.getProvinceName())
                .birthYear(user.getBirthYear())
                .isVerifiedPhone(user.getIsVerifiedPhone())
                .build();
    }
}
