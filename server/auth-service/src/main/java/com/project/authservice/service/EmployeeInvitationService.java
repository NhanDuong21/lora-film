package com.project.authservice.service;

import com.project.authservice.client.NotificationClient;
import com.project.authservice.entity.Account;
import com.project.authservice.entity.PasswordResetToken;
import com.project.authservice.repository.PasswordResetTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class EmployeeInvitationService {
    private final PasswordResetTokenRepository tokenRepository;
    private final NotificationClient notificationClient;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmployeeInvitationService(PasswordResetTokenRepository tokenRepository,
                                     NotificationClient notificationClient) {
        this.tokenRepository = tokenRepository;
        this.notificationClient = notificationClient;
    }

    @Transactional
    public LocalDateTime issue(Account account, String fullName) {
        invalidate(account.getId());
        String otp = String.format(java.util.Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000));
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(48);
        tokenRepository.save(PasswordResetToken.builder()
                .account(account)
                .otpCode(otp)
                .expiredAt(expiresAt)
                .isUsed(false)
                .purpose("EMPLOYEE_INVITATION")
                .attempts(0)
                .build());
        notificationClient.sendEmployeeInvitation(account.getId(), account.getEmail(), fullName, otp);
        return expiresAt;
    }

    @Transactional
    public void invalidate(Long accountId) {
        LocalDateTime now = LocalDateTime.now();
        tokenRepository.findByAccountIdAndIsUsedFalse(accountId).forEach(token -> {
            token.setIsUsed(true);
            token.setUsedAt(now);
        });
    }

    @Transactional(readOnly = true)
    public LocalDateTime expiry(Account account) {
        if (account == null || account.getId() == null) {
            return null;
        }
        return tokenRepository
                .findFirstByAccountIdAndIsUsedFalseOrderByCreatedAtDesc(account.getId())
                .filter(token -> "EMPLOYEE_INVITATION".equals(token.getPurpose()))
                .map(PasswordResetToken::getExpiredAt)
                .orElse(null);
    }
}
