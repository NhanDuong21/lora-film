package com.project.userservice.mapper;

import com.project.userservice.dto.response.CustomerResponse;
import com.project.userservice.entity.CustomerProfile;
import com.project.userservice.entity.User;
import com.project.userservice.exception.BusinessException;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public CustomerResponse toResponse(CustomerProfile profile, User user) {
        if (user == null) {
            throw new BusinessException("User not found", "USER_001");
        }
        return new CustomerResponse(profile.getId(), user.getAccountId(), profile.getCustomerCode(),
                user.getFullName(), user.getEmail(), user.getPhoneNumber(), user.getGender(),
                user.getBirthday(), user.getAvatarUrl(), user.getStatus(), profile.getJoinedAt(),
                profile.getNote());
    }
}
