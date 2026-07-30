package com.project.userservice.mapper;

import com.project.userservice.dto.response.UserProfileResponse;
import com.project.userservice.entity.User;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class UserProfileMapper {

    public UserProfileResponse toResponse(User user, String customerCode) {
        UserProfileResponse response = new UserProfileResponse();
        response.setAccountId(user.getAccountId());
        response.setCustomerCode(customerCode == null
                ? formatCustomerCode(user.getAccountId())
                : customerCode);
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setGender(user.getGender());
        response.setBirthday(user.getBirthday());
        response.setCccdMasked(user.getCccdMasked());
        response.setProvinceName(user.getProvinceName());
        response.setBirthYear(user.getBirthYear());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setStatus(user.getStatus());
        return response;
    }

    private String formatCustomerCode(Long accountId) {
        return accountId == null ? null : String.format(Locale.ROOT, "CUS%010d", accountId);
    }
}
