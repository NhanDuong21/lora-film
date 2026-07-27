package com.project.authservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.authservice.client.CccdCheckClient;
import com.project.authservice.dto.request.RegisterRequest;
import com.project.authservice.entity.PendingRegistrationData;
import com.project.authservice.repository.AccountRepository;
import com.project.authservice.repository.LoginHistoryRepository;
import com.project.authservice.repository.PasswordResetTokenRepository;
import com.project.authservice.repository.RefreshTokenRepository;
import com.project.authservice.repository.RoleRepository;
import com.project.authservice.repository.UserSessionRepository;
import com.project.authservice.service.impl.AuthServiceImpl;
import com.project.authservice.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationSecurityTest {
    @Mock AccountRepository accountRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @Mock CccdCheckClient cccdCheckClient;
    @Mock VerificationService verificationService;
    @Mock AuditLogService auditLogService;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock HttpServletRequest servletRequest;
    @Mock com.project.authservice.event.publisher.AuthAccountEventPublisher eventPublisher;
    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOperations;
    @Mock UserSessionRepository userSessionRepository;
    @Mock LoginHistoryRepository loginHistoryRepository;
    @Mock PasswordResetTokenRepository passwordResetTokenRepository;

    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(accountRepository, roleRepository, passwordEncoder, jwtUtil,
                cccdCheckClient, verificationService, auditLogService, refreshTokenRepository,
                servletRequest, eventPublisher, redisTemplate, new ObjectMapper(),
                userSessionRepository, loginHistoryRepository, passwordResetTokenRepository);
    }

    @Test
    void pendingRegistrationContainsOnlyPasswordHash() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String plaintext = "Password@123";
        String passwordHash = "$2a$10$abcdefghijklmnopqrstuv012345678901234567890123456789";
        RegisterRequest request = new RegisterRequest(
                "Nguyen Van A", "TEST@example.com", "0901234567",
                "092205006789", "2005-06-12", plaintext);

        when(accountRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(cccdCheckClient.checkCccd(request.getCccd())).thenReturn(
                new CccdCheckClient.CccdInfo(
                        "092******789", "092", "Test Province", "MALE", 2005, null));
        when(passwordEncoder.encode(plaintext)).thenReturn(passwordHash);

        service.register(request);

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(
                org.mockito.ArgumentMatchers.startsWith("temp_request:"),
                json.capture(),
                org.mockito.ArgumentMatchers.eq(Duration.ofMinutes(5)));
        assertThat(json.getValue()).doesNotContain(plaintext).contains(passwordHash);
    }

    @Test
    void pendingRegistrationRoundTripsThroughRedisJson() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RegisterRequest request = new RegisterRequest(
                "Nguyen Van A", "test@example.com", "0901234567",
                "092205006789", "2005-06-12", "$2a$10$storedHash");
        CccdCheckClient.CccdInfo cccdInfo = new CccdCheckClient.CccdInfo(
                "092******789", "092", "Test Province", "MALE", 2005, "valid");

        String json = objectMapper.writeValueAsString(new PendingRegistrationData(request, cccdInfo));
        PendingRegistrationData restored = objectMapper.readValue(json, PendingRegistrationData.class);

        assertThat(restored.getRequest().getEmail()).isEqualTo("test@example.com");
        assertThat(restored.getRequest().getPassword()).isEqualTo("$2a$10$storedHash");
        assertThat(restored.getCccdInfo().getProvinceCode()).isEqualTo("092");
        assertThat(restored.getCccdInfo().getBirthYear()).isEqualTo(2005);
    }
}
