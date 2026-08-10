package com.project.userservice.service;

import com.project.userservice.dto.response.CustomerResponse;
import com.project.userservice.dto.response.CustomerCounterLookupResponse;
import com.project.userservice.dto.request.CustomerAccessActionRequest;
import com.project.userservice.entity.CustomerProfile;
import com.project.userservice.entity.User;
import com.project.userservice.enumtype.UserStatus;
import com.project.userservice.enumtype.AccountType;
import com.project.userservice.exception.BusinessException;
import com.project.userservice.mapper.CustomerMapper;
import com.project.userservice.repository.CustomerProfileRepository;
import com.project.userservice.repository.EmployeeRepository;
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
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final UserAuditService auditService;
    private final UserDomainEventService eventService;
    private final CustomerMapper customerMapper;

    public CustomerService(CustomerProfileRepository customerRepository, EmployeeRepository employeeRepository,
                           UserRepository userRepository,
                           UserAuditService auditService, UserDomainEventService eventService,
                           CustomerMapper customerMapper) {
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.eventService = eventService;
        this.customerMapper = customerMapper;
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
        return page.map(profile -> customerMapper.toResponse(
                profile, users.get(profile.getAccountId())));
    }

    @Transactional(readOnly = true)
    public Page<CustomerCounterLookupResponse> searchForCounter(String keyword, Pageable pageable) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.length() < 3) {
            throw new BusinessException(
                    "Enter at least 3 characters to search for a customer",
                    "USER_COUNTER_SEARCH_TOO_SHORT");
        }
        Page<CustomerProfile> page = customerRepository.search(
                normalizedKeyword,
                UserStatus.ACTIVE,
                com.project.userservice.util.PageableUtils.sanitize(
                        pageable,
                        java.util.Set.of("id", "customerCode", "joinedAt", "createdAt"),
                        "createdAt",
                        org.springframework.data.domain.Sort.Direction.DESC));
        Map<Long, User> users = userRepository.findAllById(
                        page.getContent().stream().map(CustomerProfile::getAccountId).toList())
                .stream()
                .filter(user -> user.getAccountType() != AccountType.WORKFORCE)
                .collect(Collectors.toMap(User::getAccountId, Function.identity()));
        return page.map(profile -> {
            User user = users.get(profile.getAccountId());
            if (user == null) {
                throw new BusinessException("Customer account is unavailable", "USER_CUSTOMER_PERSONA_INACTIVE");
            }
            return new CustomerCounterLookupResponse(
                    user.getAccountId(),
                    profile.getCustomerCode(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getPhoneNumber(),
                    user.getStatus());
        });
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(Long id) {
        CustomerProfile profile = customerRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Customer not found", "USER_002"));
        ensureActiveCustomerPersona(profile);
        User user = userRepository.findById(profile.getAccountId())
                .orElseThrow(() -> new BusinessException("User not found", "USER_001"));
        return customerMapper.toResponse(profile, user);
    }

    @Transactional
    @CacheEvict(value = "userDashboard", allEntries = true)
    public CustomerResponse changeStatus(Long id, UserStatus status) {
        return changeStatus(id, status, null);
    }

    @Transactional
    @CacheEvict(value = "userDashboard", allEntries = true)
    public CustomerResponse applyAccessAction(Long id, CustomerAccessActionRequest request) {
        UserStatus target = request.type() == com.project.userservice.enumtype.CustomerAccessActionType.BLOCK
                ? UserStatus.BLOCKED : UserStatus.ACTIVE;
        return changeStatus(id, target, request.reason().trim());
    }

    private CustomerResponse changeStatus(Long id, UserStatus status, String reason) {
        CustomerProfile profile = customerRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Customer not found", "USER_002"));
        ensureActiveCustomerPersona(profile);
        User user = userRepository.findById(profile.getAccountId())
                .orElseThrow(() -> new BusinessException("User not found", "USER_001"));
        user.setStatus(status);
        userRepository.save(user);
        String event = status == UserStatus.BLOCKED ? "CUSTOMER_BLOCKED" : "CUSTOMER_UNBLOCKED";
        auditService.log(event, "CUSTOMER", id,
                reason == null ? "Legacy access action" : "reason=" + reason);
        eventService.record(event, "CUSTOMER", id, Map.of("customerId", id, "accountId", user.getAccountId()));
        return customerMapper.toResponse(profile, user);
    }

    private void ensureActiveCustomerPersona(CustomerProfile profile) {
        boolean workforceType = userRepository.findById(profile.getAccountId())
                .map(user -> user.getAccountType() == AccountType.WORKFORCE)
                .orElse(false);
        if (workforceType || employeeRepository.existsByAccountIdAndIsDeletedFalse(profile.getAccountId())) {
            throw new BusinessException(
                    "Workforce accounts cannot be managed from the customer console",
                    "USER_CUSTOMER_PERSONA_INACTIVE");
        }
    }
}
