package com.project.authservice.repository;

import com.project.authservice.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByOtpCode(String otpCode);

    List<PasswordResetToken> findAllByOtpCodeAndIsUsedFalseOrderByCreatedAtDesc(String otpCode);

    List<PasswordResetToken> findByAccountIdAndIsUsedFalse(Long accountId);
}
