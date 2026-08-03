package com.project.authservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.authservice.client.CccdCheckClient;
import com.project.authservice.dto.request.RegisterRequest;
import com.project.authservice.dto.request.SendOtpRequest;
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
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
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
    @Mock CredentialRevocationService credentialRevocationService;
    @Mock AuthOutboxService authOutboxService;

    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(accountRepository, roleRepository, passwordEncoder, jwtUtil,
                cccdCheckClient, verificationService, auditLogService, refreshTokenRepository,
                servletRequest, eventPublisher, redisTemplate, new ObjectMapper(),
                userSessionRepository, loginHistoryRepository, passwordResetTokenRepository,
                credentialRevocationService, authOutboxService);
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
        
        com.project.authservice.entity.Role mockRole = new com.project.authservice.entity.Role();
        when(roleRepository.findByRoleName(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(mockRole));
        when(accountRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(i -> i.getArgument(0));

        org.mockito.Mockito.doAnswer(invocation -> {
            String reqId = invocation.getArgument(1);
            service.completeValidation(reqId, new com.project.authservice.dto.ValidationResult("SUCCESS", null, null));
            return null;
        }).when(eventPublisher).publishRegistrationValidationRequested(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());

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

    @Test
    void failedOtpDispatchRemovesPendingStateAndReleasesIdentityReservation() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        RegisterRequest request = new RegisterRequest(
                "Nguyen Van A", "TEST@example.com", "0901234567",
                "092205006789", "2005-06-12", "Password@123");

        when(accountRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(cccdCheckClient.checkCccd(request.getCccd())).thenReturn(
                new CccdCheckClient.CccdInfo(
                        "092******789", "092", "Test Province", "MALE", 2005, null));
        when(passwordEncoder.encode("Password@123")).thenReturn("$2a$10$storedHash");

        com.project.authservice.entity.Role role = new com.project.authservice.entity.Role();
        when(roleRepository.findByRoleName(anyString())).thenReturn(Optional.of(role));
        when(accountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.doAnswer(invocation -> {
            String requestId = invocation.getArgument(1);
            service.completeValidation(
                    requestId,
                    new com.project.authservice.dto.ValidationResult("SUCCESS", null, null));
            return null;
        }).when(eventPublisher).publishRegistrationValidationRequested(any(), anyString());
        doThrow(new RuntimeException("mail unavailable"))
                .when(verificationService).sendOtp(any(SendOtpRequest.class));

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(RuntimeException.class);

        verify(redisTemplate).delete("pending_registration:test@example.com");
        verify(redisTemplate).execute(
                isA(RedisScript.class),
                eq(List.of(
                        "reserved_phone:0901234567",
                        "reserved_cccd:092205006789")),
                eq("test@example.com"));
    }
}
