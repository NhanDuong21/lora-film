package com.project.userservice.service;

import com.project.userservice.dto.response.NotificationRecipientResponse;
import com.project.userservice.entity.User;
import com.project.userservice.exception.BusinessException;
import com.project.userservice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationRecipientService {

    private final UserRepository userRepository;

    public NotificationRecipientService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public NotificationRecipientResponse findByAccountId(Long accountId) {
        User user = userRepository.findById(accountId)
                .filter(item -> !Boolean.TRUE.equals(item.getIsDeleted()))
                .orElseThrow(() -> new BusinessException(
                        "User notification recipient was not found", "USER_NOT_FOUND"));
        return new NotificationRecipientResponse(
                user.getAccountId(),
                user.getEmail(),
                user.getFullName());
    }
}
