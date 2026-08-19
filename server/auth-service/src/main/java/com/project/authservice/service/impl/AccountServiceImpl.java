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
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

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
    private final com.project.authservice.repository.PasswordResetTokenRepository passwordResetTokenRepository;
    private final com.project.authservice.client.NotificationClient notificationClient;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional(readOnly = true)
    public Page<AccountDto> getAccounts(String keyword, AccountStatus status, Long roleId,
                                        String accountScope, Pageable pageable) {
        Pageable safePageable = sanitize(pageable);
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        String normalizedScope = accountScope == null || accountScope.isBlank()
                ? null : accountScope.trim().toUpperCase(java.util.Locale.ROOT);
        if (normalizedScope != null && !java.util.Set.of("INTERNAL", "CUSTOMER").contains(normalizedScope)) {
            throw new com.project.authservice.exception.BusinessException("Phạm vi tài khoản không hợp lệ");
        }
        return accountRepository.search(normalizedKeyword, status, roleId, normalizedScope, safePageable)
                .map(this::mapToDto);
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
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        if (java.util.Objects.equals(id, currentActorAccountId()) && status != AccountStatus.ACTIVE) {
            throw new com.project.authservice.exception.BusinessException(
                    "Bạn không thể tự khóa hoặc thu hồi tài khoản đang sử dụng");
        }
        if (status != AccountStatus.ACTIVE && isLastActiveAdministrator(account)) {
            throw new com.project.authservice.exception.BusinessException(
                    "Không thể khóa hoặc thu hồi quản trị viên hoạt động cuối cùng");
        }
        if (Boolean.TRUE.equals(account.getIsDeleted()) && status != AccountStatus.DELETED) {
            throw new com.project.authservice.exception.BusinessException("Tài khoản đã thu hồi không thể kích hoạt lại");
        }
        AccountStatus previousStatus = account.getAccountStatus();
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
        auditLogService.log(account.getId(), "UPDATE_ACCOUNT_STATUS", servletRequest,
                account.getId().toString(), "before=" + previousStatus + ",after=" + status);
        return mapToDto(account);
    }

    @Override
    @Transactional
    public AccountDto updateAccountRole(Long id, Long roleId, String reason) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò"));

        if (account.getRole() != null && account.getRole().getId().equals(role.getId())) {
            throw new com.project.authservice.exception.BusinessException("Tài khoản đã thuộc vai trò này");
        }

        Role previousRoleEntity = account.getRole();
        if (previousRoleEntity != null && "CUSTOMER".equals(previousRoleEntity.getCode())
                && !"CUSTOMER".equals(role.getCode())) {
            throw new com.project.authservice.exception.BusinessException(
                    "Khách hàng chỉ có thể trở thành nhân viên qua quy trình hồ sơ nhân sự và lời mời nội bộ");
        }
        boolean removingAdministrator = isAdminRole(previousRoleEntity) && !isAdminRole(role);
        if (removingAdministrator && java.util.Objects.equals(id, currentActorAccountId())) {
            throw new com.project.authservice.exception.BusinessException(
                    "Bạn không thể tự thu hồi quyền quản trị của tài khoản đang sử dụng");
        }
        if (removingAdministrator && isLastActiveAdministrator(account)) {
            throw new com.project.authservice.exception.BusinessException(
                    "Không thể hạ quyền quản trị viên hoạt động cuối cùng");
        }
        String normalizedReason = reason == null ? "" : reason.trim();
        if (!isAdminRole(previousRoleEntity) && isAdminRole(role) && normalizedReason.length() < 5) {
            throw new com.project.authservice.exception.BusinessException(
                    "Vui lòng nhập lý do khi cấp quyền Quản trị hệ thống");
        }
        if (normalizedReason.length() > 500) {
            throw new com.project.authservice.exception.BusinessException("Lý do không được vượt quá 500 ký tự");
        }

        String previousRole = previousRoleEntity == null ? "NONE" : previousRoleEntity.getCode();
        account.setRole(role);
        if (!"EMPLOYEE".equals(role.getCode())) {
            account.setAccessProfile(null);
        }
        if (!"MANAGER".equals(role.getCode())) {
            account.setAssignedCinemaPublicIds(java.util.Set.of());
        }
        account = accountRepository.save(account);
        
        credentialRevocationService.revokeAll(account.getId());
        String roleCode = role.getCode() == null ? role.getRoleName() : role.getCode();
        authOutboxService.record("ACCOUNT_ROLE_CHANGED", account.getId(),
                java.util.Map.of("accountId", account.getId(), "role", roleCode));
        auditLogService.log(account.getId(), "UPDATE_ACCOUNT_ROLE", servletRequest,
                account.getId().toString(), "before=" + previousRole + ",after=" + roleCode
                        + (normalizedReason.isBlank() ? "" : ",reason=" + normalizedReason));
        return mapToDto(account);
    }

    @Override
    @Transactional
    public AccountDto updateManagerCinemaAssignments(Long id, java.util.Set<String> cinemaPublicIds) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        if (account.getRole() == null || !"MANAGER".equals(account.getRole().getCode())) {
            throw new com.project.authservice.exception.BusinessException(
                    "Cinema assignments can only be applied to MANAGER accounts");
        }

        java.util.Set<String> normalizedIds = cinemaPublicIds == null
                ? java.util.Set.of()
                : cinemaPublicIds.stream()
                        .filter(java.util.Objects::nonNull)
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .map(value -> value.toLowerCase(java.util.Locale.ROOT))
                        .sorted()
                        .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        if (account.getAssignedCinemaPublicIds().equals(normalizedIds)) {
            return mapToDto(account);
        }

        java.util.Set<String> previousIds = new java.util.LinkedHashSet<>(account.getAssignedCinemaPublicIds());
        account.setAssignedCinemaPublicIds(normalizedIds);
        account = accountRepository.save(account);
        credentialRevocationService.revokeAll(account.getId());
        authOutboxService.record("MANAGER_CINEMA_ASSIGNMENTS_CHANGED", account.getId(),
                java.util.Map.of(
                        "accountId", account.getId(),
                        "cinemaPublicIds", normalizedIds));
        auditLogService.log(account.getId(), "UPDATE_MANAGER_CINEMA_ASSIGNMENTS", servletRequest,
                account.getId().toString(), "before=" + previousIds + ",after=" + normalizedIds);
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
        String previousProfile = account.getAccessProfile() == null
                ? "NONE" : account.getAccessProfile().getName();
        account.setAccessProfile(profile);
        account = accountRepository.save(account);
        credentialRevocationService.revokeAll(account.getId());
        authOutboxService.record("ACCOUNT_ACCESS_PROFILE_CHANGED", account.getId(),
                java.util.Map.of(
                        "accountId", account.getId(),
                        "role", "EMPLOYEE",
                        "accessProfile", profile.getCode()));
        auditLogService.log(account.getId(), "UPDATE_ACCOUNT_ACCESS_PROFILE", servletRequest,
                account.getId().toString(), "before=" + previousProfile + ",after=" + profile.getName());
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
                .assignedCinemaPublicIds(account.getAssignedCinemaPublicIds())
                .enabled(account.getIsEnabled())
                .status(account.getAccountStatus())
                .lastLoginAt(account.getLastLoginAt())
                .invitationExpiresAt(invitationExpiry(account))
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
            throw new com.project.authservice.exception.BusinessException("Email này đã có tài khoản trên hệ thống");
        }

        Role role = roleRepository.findByCode("EMPLOYEE")
                .orElseThrow(() -> new ResourceNotFoundException("Chưa cấu hình vai trò Nhân viên"));
        com.project.authservice.entity.AccessProfile accessProfile = accessProfileRepository
                .findById(request.getAccessProfileId())
                .filter(profile -> Boolean.TRUE.equals(profile.getActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhóm nghiệp vụ đã chọn"));

        Account account = new Account();
        account.setEmail(email);
        account.setPasswordHash(passwordEncoder.encode(randomUnavailablePassword()));
        account.setRole(role);
        account.setAccessProfile(accessProfile);
        account.setAccountStatus(AccountStatus.INACTIVE);
        account.setIsEnabled(false);
        account.setIsDeleted(false);
        
        account = accountRepository.save(account);

        eventPublisher.publishEmployeeAccountCreated(account, request.getFullName());

        createAndSendInvitation(account, request.getFullName());
        auditLogService.log(account.getId(), "CREATE_EMPLOYEE_INVITATION", servletRequest,
                account.getId().toString(), "after=Chờ kích hoạt,invitationExpiresAt=" + invitationExpiry(account));

        return mapToDto(account);
    }

    @Override
    @Transactional
    public AccountDto resendEmployeeInvitation(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        if (account.getAccountStatus() != AccountStatus.INACTIVE
                || Boolean.TRUE.equals(account.getIsDeleted())
                || account.getRole() == null
                || !"EMPLOYEE".equals(account.getRole().getCode())) {
            throw new com.project.authservice.exception.BusinessException(
                    "Chỉ có thể gửi lại lời mời cho tài khoản nhân viên đang chờ kích hoạt");
        }
        createAndSendInvitation(account, null);
        auditLogService.log(account.getId(), "RESEND_EMPLOYEE_INVITATION", servletRequest,
                account.getId().toString(), "invitationExpiresAt=" + invitationExpiry(account));
        return mapToDto(account);
    }

    @Override
    @Transactional
    public void sendPasswordReset(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        if (account.getAccountStatus() != AccountStatus.ACTIVE || Boolean.TRUE.equals(account.getIsDeleted())) {
            throw new com.project.authservice.exception.BusinessException(
                    "Chỉ có thể gửi email đặt lại mật khẩu cho tài khoản đang hoạt động");
        }
        invalidateUnusedResetTokens(account.getId());
        String otp = sixDigitOtp();
        passwordResetTokenRepository.save(com.project.authservice.entity.PasswordResetToken.builder()
                .account(account)
                .otpCode(otp)
                .expiredAt(LocalDateTime.now().plusMinutes(15))
                .isUsed(false)
                .purpose("PASSWORD_RESET")
                .attempts(0)
                .build());
        notificationClient.sendForgotPasswordOtp(account.getId(), account.getEmail(), otp);
        auditLogService.log(account.getId(), "ADMIN_SENT_PASSWORD_RESET", servletRequest);
    }

    @Override
    @Transactional
    public void revokeAllSessions(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        if (java.util.Objects.equals(id, currentActorAccountId())) {
            throw new com.project.authservice.exception.BusinessException(
                    "Bạn không thể tự đăng xuất tài khoản đang sử dụng khỏi tất cả thiết bị");
        }
        credentialRevocationService.revokeAll(account.getId());
        auditLogService.log(account.getId(), "ADMIN_REVOKED_ALL_SESSIONS", servletRequest);
    }

    private boolean isLastActiveAdministrator(Account account) {
        return isAdminRole(account.getRole())
                && account.getAccountStatus() == AccountStatus.ACTIVE
                && Boolean.TRUE.equals(account.getIsEnabled())
                && !Boolean.TRUE.equals(account.getIsDeleted())
                && accountRepository.countActiveAdministrators() <= 1;
    }

    private boolean isAdminRole(Role role) {
        if (role == null || role.getCode() == null) return false;
        return java.util.Set.of("ADMIN", "ROLE_ADMIN").contains(role.getCode().toUpperCase(java.util.Locale.ROOT));
    }

    private Long currentActorAccountId() {
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return null;
        Object credentials = authentication.getCredentials();
        if (credentials instanceof Number number) return number.longValue();
        if (credentials instanceof String value) {
            try {
                return Long.valueOf(value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private void createAndSendInvitation(Account account, String fullName) {
        invalidateUnusedResetTokens(account.getId());
        String otp = sixDigitOtp();
        passwordResetTokenRepository.save(com.project.authservice.entity.PasswordResetToken.builder()
                .account(account)
                .otpCode(otp)
                .expiredAt(LocalDateTime.now().plusHours(48))
                .isUsed(false)
                .purpose("EMPLOYEE_INVITATION")
                .attempts(0)
                .build());
        notificationClient.sendEmployeeInvitation(account.getId(), account.getEmail(), fullName, otp);
    }

    private void invalidateUnusedResetTokens(Long accountId) {
        passwordResetTokenRepository.findByAccountIdAndIsUsedFalse(accountId).forEach(token -> {
            token.setIsUsed(true);
            token.setUsedAt(LocalDateTime.now());
        });
    }

    private LocalDateTime invitationExpiry(Account account) {
        if (account.getAccountStatus() != AccountStatus.INACTIVE) return null;
        return passwordResetTokenRepository
                .findFirstByAccountIdAndIsUsedFalseOrderByCreatedAtDesc(account.getId())
                .map(com.project.authservice.entity.PasswordResetToken::getExpiredAt)
                .orElse(null);
    }

    private String randomUnavailablePassword() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sixDigitOtp() {
        return String.format(java.util.Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000));
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
                              AccessProfileRepository accessProfileRepository,
                              com.project.authservice.repository.PasswordResetTokenRepository passwordResetTokenRepository,
                              com.project.authservice.client.NotificationClient notificationClient) {
        this.accountRepository = accountRepository;
        this.roleRepository = roleRepository;
        this.auditLogService = auditLogService;
        this.servletRequest = servletRequest;
        this.credentialRevocationService = credentialRevocationService;
        this.authOutboxService = authOutboxService;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.accessProfileRepository = accessProfileRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.notificationClient = notificationClient;
    }
}
