package com.project.userservice.service.impl;

import com.project.userservice.dto.response.UserProfileResponse;
import com.project.userservice.dto.request.UpdateProfileRequest;
import com.project.userservice.entity.User;
import com.project.userservice.exception.BusinessException;
import com.project.userservice.mapper.UserProfileMapper;
import com.project.userservice.repository.UserRepository;
import com.project.userservice.repository.CustomerProfileRepository;
import com.project.userservice.service.UserService;
import com.project.userservice.service.UserAuditService;
import com.project.userservice.service.UserDomainEventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserAuditService auditService;
    private final UserDomainEventService eventService;
    private final CustomerProfileRepository customerProfileRepository;
    private final UserProfileMapper userProfileMapper;

    public UserServiceImpl(UserRepository userRepository, UserAuditService auditService,
                           UserDomainEventService eventService,
                           CustomerProfileRepository customerProfileRepository,
                           UserProfileMapper userProfileMapper) {
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.eventService = eventService;
        this.customerProfileRepository = customerProfileRepository;
        this.userProfileMapper = userProfileMapper;
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
            if (request.birthday().plusYears(13).isAfter(java.time.LocalDate.now())) {
                throw new BusinessException("User must be at least 13 years old", "USER_008");
            }
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
        List<User> orderedUsers = accountIds.stream()
                .distinct()
                .map(usersById::get)
                .filter(java.util.Objects::nonNull)
                .filter(user -> !Boolean.TRUE.equals(user.getIsDeleted()))
                .toList();
        return mapToResponses(orderedUsers);
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

        List<User> users = matches.values().stream()
                .limit(safeLimit)
                .toList();
        return mapToResponses(users);
    }

    private UserProfileResponse mapToResponse(User user) {
        String customerCode = customerProfileRepository.findByAccountId(user.getAccountId())
                .map(com.project.userservice.entity.CustomerProfile::getCustomerCode)
                .orElse(null);
        return userProfileMapper.toResponse(user, customerCode);
    }

    private List<UserProfileResponse> mapToResponses(List<User> users) {
        if (users.isEmpty()) {
            return List.of();
        }
        List<Long> accountIds = users.stream()
                .map(User::getAccountId)
                .toList();
        Map<Long, String> customerCodes = customerProfileRepository.findByAccountIdIn(accountIds)
                .stream()
                .collect(Collectors.toMap(
                        com.project.userservice.entity.CustomerProfile::getAccountId,
                        com.project.userservice.entity.CustomerProfile::getCustomerCode,
                        (first, ignored) -> first));
        return users.stream()
                .map(user -> userProfileMapper.toResponse(
                        user, customerCodes.get(user.getAccountId())))
                .toList();
    }

    private java.util.Optional<Long> parseAccountId(String query) {
        String candidate = query.toUpperCase(Locale.ROOT);
        if (candidate.startsWith("CUS")) {
            candidate = candidate.substring(3);
        } else if (candidate.startsWith("KH")) {
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
