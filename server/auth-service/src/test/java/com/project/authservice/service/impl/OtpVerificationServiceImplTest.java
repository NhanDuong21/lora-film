package com.project.authservice.service.impl;

import com.project.authservice.client.NotificationClient;
import com.project.authservice.dto.request.SendOtpRequest;
import com.project.authservice.entity.Account;
import com.project.authservice.enums.AccountStatus;
import com.project.authservice.exception.OtpDeliveryFailedException;
import com.project.authservice.exception.RegistrationExpiredException;
import com.project.authservice.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtpVerificationServiceImplTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private NotificationClient notificationClient;

    private OtpVerificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OtpVerificationServiceImpl(
                accountRepository, redisTemplate, passwordEncoder, notificationClient);
    }

    @Test
    void failedDeliveryRemovesOtpAndCooldownSoResendCanBeImmediate() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Account account = inactiveAccount();
        when(accountRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(account));
        when(redisTemplate.hasKey("pending_registration:customer@example.com")).thenReturn(true);
        when(valueOperations.get("otp:customer@example.com")).thenReturn(null);
        when(valueOperations.get("pending_registration:customer@example.com")).thenReturn(null);
        when(passwordEncoder.encode(anyString())).thenReturn("otp-hash");
        org.mockito.Mockito.doThrow(new OtpDeliveryFailedException("SMTP_POLICY_REJECTED"))
                .when(notificationClient)
                .sendRegistrationOtp(eq(42L), eq("customer@example.com"), eq("Khách hàng"), anyString());

        assertThatThrownBy(() -> service.sendOtp(new SendOtpRequest("Customer@Example.com")))
                .isInstanceOf(OtpDeliveryFailedException.class);

        verify(valueOperations).set(eq("otp:customer@example.com"), anyString(), eq(Duration.ofMinutes(5)));
        verify(redisTemplate).delete("otp:customer@example.com");
    }

    @Test
    void expiredRegistrationDoesNotCreateOrSendAnOtp() {
        when(accountRepository.findByEmail("customer@example.com"))
                .thenReturn(Optional.of(inactiveAccount()));
        when(redisTemplate.hasKey("pending_registration:customer@example.com")).thenReturn(false);

        assertThatThrownBy(() -> service.sendOtp(new SendOtpRequest("customer@example.com")))
                .isInstanceOf(RegistrationExpiredException.class);

        verify(passwordEncoder, never()).encode(anyString());
        verify(notificationClient, never()).sendRegistrationOtp(any(), anyString(), anyString(), anyString());
    }

    private Account inactiveAccount() {
        Account account = new Account();
        account.setId(42L);
        account.setAccountStatus(AccountStatus.INACTIVE);
        return account;
    }
}
