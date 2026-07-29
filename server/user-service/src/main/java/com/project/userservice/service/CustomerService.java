package com.project.userservice.service;

import com.project.userservice.dto.response.CustomerResponse;
import com.project.userservice.entity.CustomerProfile;
import com.project.userservice.entity.User;
import com.project.userservice.enumtype.UserStatus;
import com.project.userservice.exception.BusinessException;
import com.project.userservice.repository.CustomerProfileRepository;
import com.project.userservice.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CustomerService {
    private final CustomerProfileRepository customerRepository;
    private final UserRepository userRepository;
    private final UserAuditService auditService;
    private final UserDomainEventService eventService;

    public CustomerService(CustomerProfileRepository customerRepository, UserRepository userRepository,
                           UserAuditService auditService, UserDomainEventService eventService) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.eventService = eventService;
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> search(String keyword, UserStatus status, Pageable pageable) {
        Page<CustomerProfile> page = customerRepository.search(keyword, status,
                com.project.userservice.util.PageableUtils.sanitize(pageable,
                        java.util.Set.of("id", "customerCode", "joinedAt", "createdAt", "updatedAt"),
                        "createdAt", org.springframework.data.domain.Sort.Direction.DESC));
        Map<Long, User> users = userRepository.findAllById(
                        page.getContent().stream().map(CustomerProfile::getAccountId).toList())
                .stream().collect(Collectors.toMap(User::getAccountId, Function.identity()));
        return page.map(profile -> map(profile, users.get(profile.getAccountId())));
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(Long id) {
        CustomerProfile profile = customerRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Customer not found", "USER_002"));
        User user = userRepository.findById(profile.getAccountId())
                .orElseThrow(() -> new BusinessException("User not found", "USER_001"));
        return map(profile, user);
    }

    @Transactional
    @CacheEvict(value = "userDashboard", allEntries = true)
    public CustomerResponse changeStatus(Long id, UserStatus status) {
        CustomerProfile profile = customerRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Customer not found", "USER_002"));
        User user = userRepository.findById(profile.getAccountId())
                .orElseThrow(() -> new BusinessException("User not found", "USER_001"));
        user.setStatus(status);
        userRepository.save(user);
        String event = status == UserStatus.BLOCKED ? "CUSTOMER_BLOCKED" : "CUSTOMER_UNBLOCKED";
        auditService.log(event, "CUSTOMER", id, null);
        eventService.record(event, "CUSTOMER", id, Map.of("customerId", id, "accountId", user.getAccountId()));
        return map(profile, user);
    }

    private CustomerResponse map(CustomerProfile profile, User user) {
        if (user == null) {
            throw new BusinessException("User not found", "USER_001");
        }
        return new CustomerResponse(profile.getId(), user.getAccountId(), profile.getCustomerCode(),
                user.getFullName(), user.getEmail(), user.getPhoneNumber(), user.getGender(), user.getBirthday(),
                user.getAvatarUrl(), user.getStatus(), profile.getJoinedAt(), profile.getNote());
    }
}
