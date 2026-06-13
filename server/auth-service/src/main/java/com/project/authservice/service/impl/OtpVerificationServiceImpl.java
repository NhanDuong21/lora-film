package com.project.authservice.service.impl;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.authservice.entity.Account;
import com.project.authservice.exception.AccountNotFoundException;
import com.project.authservice.exception.InvalidOtpException;
import com.project.authservice.exception.VerificationExpiredException;
import com.project.authservice.repository.AccountRepository;
import com.project.authservice.service.VerificationService;

@Service
public class OtpVerificationServiceImpl implements VerificationService {
    private static final Logger log = LoggerFactory.getLogger(OtpVerificationServiceImpl.class);

    private final AccountRepository accountRepository;
    private final Map<Long, OtpData> otpStorage = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public OtpVerificationServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public void generateVerification(Account account) {
        String otp = String.format("%06d", random.nextInt(1000000));
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(5);

        otpStorage.put(account.getId(), new OtpData(otp, expiryTime));

        String verificationMessage = 
                "\n==================================\n" +
                "ACCOUNT VERIFICATION REQUIRED\n" +
                "AccountId: " + account.getId() + "\n" +
                "OTP: " + otp + "\n" +
                "Verify API: POST /auth/verify\n" +
                "==================================";

        System.out.println(verificationMessage);
        log.info("Verification generated: accountId={}, otp={}", account.getId(), otp);
    }

    @Override
    @Transactional
    public void verify(Long accountId, String code) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(AccountNotFoundException::new);

        OtpData otpData = otpStorage.get(accountId);
        if (otpData == null || !otpData.getCode().equals(code)) {
            throw new InvalidOtpException();
        }

        if (otpData.isExpired()) {
            otpStorage.remove(accountId);
            throw new VerificationExpiredException();
        }

        // Verification successful
        otpStorage.remove(accountId);
        account.setRegistrationCompleted(1);
        account.setIsActive(1);
        accountRepository.save(account);
        log.info("Account id={} verified successfully.", accountId);
    }

    private static class OtpData {
        private final String code;
        private final LocalDateTime expiryTime;

        public OtpData(String code, LocalDateTime expiryTime) {
            this.code = code;
            this.expiryTime = expiryTime;
        }

        public String getCode() {
            return code;
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiryTime);
        }
    }
}
