package com.project.userservice.service.impl;

import com.project.userservice.dto.response.UserProfileResponse;
import com.project.userservice.dto.request.UpdateProfileRequest;
import com.project.userservice.entity.User;
import com.project.userservice.exception.BusinessException;
import com.project.userservice.repository.UserRepository;
import com.project.userservice.service.UserService;
import com.project.userservice.service.UserAuditService;
import com.project.userservice.service.UserDomainEventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserAuditService auditService;
    private final UserDomainEventService eventService;

    public UserServiceImpl(UserRepository userRepository, UserAuditService auditService,
                           UserDomainEventService eventService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.eventService = eventService;
    }

    @Override
    public UserProfileResponse getUserProfile(Long accountId) {
        User user = userRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException("User profile not found", "USER_NOT_FOUND"));
        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new BusinessException("User profile not found", "USER_NOT_FOUND");
        }
        return mapToResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(Long accountId, UpdateProfileRequest request) {
        User user = userRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException("User profile not found", "USER_NOT_FOUND"));
        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new BusinessException("User profile not found", "USER_NOT_FOUND");
        }
        if (request.fullName() != null) {
            user.setFullName(request.fullName().trim());
        }
        if (request.phoneNumber() != null) {
            String phone = request.phoneNumber().trim();
            if (userRepository.existsByPhoneNumberAndAccountIdNot(phone, accountId)) {
                throw new BusinessException("Phone number already exists", "USER_PHONE_ALREADY_EXISTS");
            }
            user.setPhoneNumber(phone);
        }
        if (request.gender() != null) {
            user.setGender(request.gender());
        }
        if (request.birthday() != null) {
            user.setBirthday(request.birthday());
            user.setBirthYear(request.birthday().getYear());
        }
        userRepository.save(user);
        auditService.log("USER_PROFILE_UPDATED", "USER", accountId, null);
        eventService.record("CUSTOMER_UPDATED", "USER", accountId,
                Map.of("accountId", accountId));
        return mapToResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getUserProfiles(List<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return List.of();
        }
        Map<Long, User> usersById = new LinkedHashMap<>();
        userRepository.findAllById(accountIds).forEach(
                user -> usersById.put(user.getAccountId(), user));
        return accountIds.stream()
                .distinct()
                .map(usersById::get)
                .filter(java.util.Objects::nonNull)
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileResponse> searchUserProfiles(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        int safeLimit = Math.max(1, Math.min(limit, 50));
        String normalized = query.trim();
        Map<Long, User> matches = new LinkedHashMap<>();
        parseAccountId(normalized).flatMap(userRepository::findById)
                .ifPresent(user -> matches.put(user.getAccountId(), user));

        if (matches.size() < safeLimit) {
            String pattern = "%" + normalized.toLowerCase(Locale.ROOT) + "%";
            userRepository.searchOperationalProfiles(
                            pattern, PageRequest.of(0, safeLimit))
                    .forEach(user -> matches.putIfAbsent(user.getAccountId(), user));
        }

        List<UserProfileResponse> response = new ArrayList<>();
        matches.values().stream()
                .limit(safeLimit)
                .map(this::mapToResponse)
                .forEach(response::add);
        return response;
    }

    private UserProfileResponse mapToResponse(User user) {
        UserProfileResponse response = new UserProfileResponse();
        response.setAccountId(user.getAccountId());
        response.setCustomerCode(formatCustomerCode(user.getAccountId()));
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
        return accountId == null ? null : String.format(Locale.ROOT, "KH%06d", accountId);
    }

    private java.util.Optional<Long> parseAccountId(String query) {
        String candidate = query.toUpperCase(Locale.ROOT);
        if (candidate.startsWith("KH")) {
            candidate = candidate.substring(2);
        }
        if (!candidate.matches("\\d+")) {
            return java.util.Optional.empty();
        }
        try {
            return java.util.Optional.of(Long.parseLong(candidate));
        } catch (NumberFormatException ignored) {
            return java.util.Optional.empty();
        }
    }
}
