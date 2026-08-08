package com.project.authservice.service.impl;

import com.project.authservice.dto.AccountDto;
import com.project.authservice.entity.Account;
import com.project.authservice.entity.Role;
import com.project.authservice.enums.AccountStatus;
import com.project.authservice.exception.ResourceNotFoundException;
import com.project.authservice.repository.AccountRepository;
import com.project.authservice.repository.AccessProfileRepository;
import com.project.authservice.repository.RoleRepository;
import com.project.authservice.service.AccountService;
import com.project.authservice.service.AuditLogService;
import com.project.authservice.service.AuthOutboxService;
import com.project.authservice.service.CredentialRevocationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final AuditLogService auditLogService;
    private final HttpServletRequest servletRequest;
    private final CredentialRevocationService credentialRevocationService;
    private final AuthOutboxService authOutboxService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final com.project.authservice.event.publisher.AuthAccountEventPublisher eventPublisher;
    private final AccessProfileRepository accessProfileRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AccountDto> getAccounts(String keyword, AccountStatus status, Long roleId, Pageable pageable) {
        Pageable safePageable = sanitize(pageable);
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return accountRepository.search(normalizedKeyword, status, roleId, safePageable).map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDto getAccountById(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        return mapToDto(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDto getAccountByEmail(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        return mapToDto(account);
    }

    @Override
    @Transactional
    public AccountDto updateAccountStatus(Long id, AccountStatus status) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        if (Boolean.TRUE.equals(account.getIsDeleted()) && status != AccountStatus.DELETED) {
            throw new com.project.authservice.exception.BusinessException("Deleted account cannot be reactivated");
        }
        account.setAccountStatus(status);
        if (status == AccountStatus.DELETED) {
            account.setIsDeleted(true);
            account.setIsEnabled(false);
        } else if (status == AccountStatus.ACTIVE) {
            account.setIsEnabled(true);
        }
        account = accountRepository.save(account);
        
        credentialRevocationService.revokeAll(account.getId());
        authOutboxService.record("ACCOUNT_STATUS_CHANGED", account.getId(),
                java.util.Map.of("accountId", account.getId(), "status", status.name()));
        auditLogService.log(account.getId(), "UPDATE_ACCOUNT_STATUS", servletRequest);
        return mapToDto(account);
    }

    @Override
    @Transactional
    public AccountDto updateAccountRole(Long id, Long roleId) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
                
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
                
        if (account.getRole() != null && account.getRole().getId().equals(role.getId())) {
            throw new com.project.authservice.exception.BusinessException("Role is already assigned to this account");
        }
                
        account.setRole(role);
        if (!"EMPLOYEE".equals(role.getCode())) {
            account.setAccessProfile(null);
        }
        account = accountRepository.save(account);
        
        credentialRevocationService.revokeAll(account.getId());
        String roleCode = role.getCode() == null ? role.getRoleName() : role.getCode();
        authOutboxService.record("ACCOUNT_ROLE_CHANGED", account.getId(),
                java.util.Map.of("accountId", account.getId(), "role", roleCode));
        auditLogService.log(account.getId(), "UPDATE_ACCOUNT_ROLE", servletRequest);
        return mapToDto(account);
    }

    @Override
    @Transactional
    public AccountDto updateAccountAccessProfile(Long id, Long accessProfileId) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        if (account.getRole() == null || !"EMPLOYEE".equals(account.getRole().getCode())) {
            throw new com.project.authservice.exception.BusinessException(
                    "Access profiles can only be assigned to EMPLOYEE accounts");
        }
        com.project.authservice.entity.AccessProfile profile = accessProfileRepository.findById(accessProfileId)
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Access profile not found"));
        if (account.getAccessProfile() != null && account.getAccessProfile().getId().equals(profile.getId())) {
            return mapToDto(account);
        }
        account.setAccessProfile(profile);
        account = accountRepository.save(account);
        credentialRevocationService.revokeAll(account.getId());
        authOutboxService.record("ACCOUNT_ACCESS_PROFILE_CHANGED", account.getId(),
                java.util.Map.of(
                        "accountId", account.getId(),
                        "role", "EMPLOYEE",
                        "accessProfile", profile.getCode()));
        auditLogService.log(account.getId(), "UPDATE_ACCOUNT_ACCESS_PROFILE", servletRequest);
        return mapToDto(account);
    }

    private AccountDto mapToDto(Account account) {
        return AccountDto.builder()
                .id(account.getId())
                .email(account.getEmail())
                .roleName(account.getRole() == null ? null : account.getRole().getCode())
                .role(account.getRole() == null ? null : com.project.authservice.dto.RoleDto.builder()
                        .id(account.getRole().getId())
                        .code(account.getRole().getCode())
                        .name(account.getRole().getRoleName())
                        .build())
                .accessProfile(mapAccessProfile(account.getAccessProfile()))
                .enabled(account.getIsEnabled())
                .status(account.getAccountStatus())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }

    private com.project.authservice.dto.AccessProfileDto mapAccessProfile(
            com.project.authservice.entity.AccessProfile profile) {
        if (profile == null) {
            return null;
        }
        com.project.authservice.dto.AccessProfileDto dto = new com.project.authservice.dto.AccessProfileDto();
        dto.setId(profile.getId());
        dto.setCode(profile.getCode());
        dto.setName(profile.getName());
        dto.setDescription(profile.getDescription());
        dto.setActive(profile.getActive());
        dto.setPermissionIds(profile.getPermissions().stream()
                .map(com.project.authservice.entity.Permission::getId)
                .collect(java.util.stream.Collectors.toSet()));
        return dto;
    }

    @Override
    @Transactional
    public AccountDto createEmployeeAccount(com.project.authservice.dto.request.EmployeeAccountRequest request) {
        String email = request.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
        
        if (accountRepository.existsByEmail(email)) {
            throw new com.project.authservice.exception.BusinessException("Email is already registered");
        }

        Role role = roleRepository.findByCode("EMPLOYEE")
                .orElseThrow(() -> new ResourceNotFoundException("Workforce role EMPLOYEE not found"));
        com.project.authservice.entity.AccessProfile accessProfile = accessProfileRepository
                .findById(request.getAccessProfileId())
                .filter(profile -> Boolean.TRUE.equals(profile.getActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Access profile not found"));

        Account account = new Account();
        account.setEmail(email);
        account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        account.setRole(role);
        account.setAccessProfile(accessProfile);
        account.setAccountStatus(AccountStatus.ACTIVE);
        account.setIsEnabled(true);
        account.setIsDeleted(false);
        
        account = accountRepository.save(account);

        auditLogService.log(account.getId(), "CREATE_EMPLOYEE_ACCOUNT", servletRequest);
        
        eventPublisher.publishEmployeeAccountCreated(account, request.getFullName());

        return mapToDto(account);
    }
    private Pageable sanitize(Pageable pageable) {
        java.util.Set<String> allowedSorts = java.util.Set.of("id", "email", "status", "createdAt", "updatedAt");
        org.springframework.data.domain.Sort sort = pageable.getSort().stream()
                .filter(order -> allowedSorts.contains(order.getProperty()))
                .map(order -> new org.springframework.data.domain.Sort.Order(order.getDirection(), order.getProperty()))
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toList(),
                        orders -> orders.isEmpty()
                                ? org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")
                                : org.springframework.data.domain.Sort.by(orders)));
        return org.springframework.data.domain.PageRequest.of(
                Math.max(0, pageable.getPageNumber()),
                Math.min(Math.max(1, pageable.getPageSize()), 100),
                sort);
    }

    public AccountServiceImpl(AccountRepository accountRepository, RoleRepository roleRepository,
                              AuditLogService auditLogService, HttpServletRequest servletRequest,
                              CredentialRevocationService credentialRevocationService,
                              AuthOutboxService authOutboxService,
                              org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
                              com.project.authservice.event.publisher.AuthAccountEventPublisher eventPublisher,
                              AccessProfileRepository accessProfileRepository) {
        this.accountRepository = accountRepository;
        this.roleRepository = roleRepository;
        this.auditLogService = auditLogService;
        this.servletRequest = servletRequest;
        this.credentialRevocationService = credentialRevocationService;
        this.authOutboxService = authOutboxService;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.accessProfileRepository = accessProfileRepository;
    }
}
