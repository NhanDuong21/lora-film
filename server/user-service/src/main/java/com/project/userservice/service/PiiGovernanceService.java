package com.project.userservice.service;

import com.project.userservice.dto.request.PiiRetentionRequest;
import com.project.userservice.dto.response.PiiGovernanceSummaryResponse;
import com.project.userservice.entity.CustomerProfile;
import com.project.userservice.entity.User;
import com.project.userservice.exception.BusinessException;
import com.project.userservice.repository.CustomerProfileRepository;
import com.project.userservice.repository.UserRepository;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PiiGovernanceService {
    public static final int ACTIVE_KEY_VERSION = 1;

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerRepository;
    private final UserAuditService auditService;

    public PiiGovernanceService(UserRepository userRepository,
                                CustomerProfileRepository customerRepository,
                                UserAuditService auditService) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.auditService = auditService;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void protectLegacyRows() {
        List<User> legacyRows = userRepository.findByPiiKeyVersionLessThan(ACTIVE_KEY_VERSION);
        for (User user : legacyRows) {
            user.refreshPiiProtection();
        }
        if (!legacyRows.isEmpty()) {
            userRepository.saveAll(legacyRows);
            auditService.log("PII_BACKFILL_COMPLETED", "PII_GOVERNANCE", 0L,
                    "protectedProfiles=" + legacyRows.size());
        }
    }

    @Transactional(readOnly = true)
    public PiiGovernanceSummaryResponse summary() {
        LocalDate today = LocalDate.now();
        return new PiiGovernanceSummaryResponse(
                userRepository.count(),
                userRepository.countByPiiKeyVersionGreaterThanEqual(ACTIVE_KEY_VERSION),
                userRepository.countByPiiRetentionUntilLessThanEqualAndPiiErasedAtIsNull(today),
                userRepository.countByPiiErasedAtIsNotNull(),
                ACTIVE_KEY_VERSION);
    }

    @Transactional
    public void scheduleRetention(Long accountId, PiiRetentionRequest request) {
        User user = userRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException("User not found", "USER_001"));
        if (!Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new BusinessException("Retention countdown can only be set after profile deletion",
                    "USER_PII_ACTIVE_PROFILE");
        }
        user.setPiiRetentionUntil(request.retentionUntil());
        userRepository.save(user);
        auditService.log("PII_RETENTION_SCHEDULED", "USER", accountId,
                "until=" + request.retentionUntil() + "; reason=" + request.reason().trim());
    }

    @Transactional
    public int anonymizeExpiredRecords() {
        List<User> expired = userRepository
                .findByPiiRetentionUntilLessThanEqualAndPiiErasedAtIsNull(LocalDate.now());
        for (User user : expired) {
            user.setFullName("ANONYMIZED-" + user.getAccountId());
            user.setEmail(null);
            user.setPhoneNumber(null);
            user.setCccd(null);
            user.setCccdMasked(null);
            user.setProvinceCode(null);
            user.setProvinceName(null);
            user.setGender(null);
            user.setBirthday(null);
            user.setBirthYear(null);
            user.setAvatarUrl(null);
            user.setPiiErasedAt(LocalDateTime.now());
            customerRepository.findByAccountId(user.getAccountId()).ifPresent(profile -> eraseCustomerNote(profile));
            auditService.log("PII_ERASED", "USER", user.getAccountId(), "retention-expired");
        }
        userRepository.saveAll(expired);
        return expired.size();
    }

    private void eraseCustomerNote(CustomerProfile profile) {
        profile.setNote(null);
        customerRepository.save(profile);
    }
}
