package com.project.authservice.client;

import com.project.authservice.exception.common.ExternalServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class NotificationClient {

    private final RestTemplate restTemplate;
    private final String notificationServiceUrl;
    private final String internalToken;

    public NotificationClient(
            RestTemplate restTemplate,
            @Value("${app.notification-service.url}") String notificationServiceUrl,
            @Value("${app.internal-token}") String internalToken) {
        this.restTemplate = restTemplate;
        this.notificationServiceUrl = notificationServiceUrl;
        this.internalToken = internalToken;
    }

    public void sendRegistrationOtp(Long accountId, String email, String name, String otp) {
        sendOtp(
                accountId,
                email,
                "AUTH_REGISTRATION_OTP",
                "REGISTER_OTP",
                5,
                Map.of(
                        "user_name", fallbackName(name),
                        "otp_code", otp,
                        "expiry_minutes", 5));
    }

    public void sendForgotPasswordOtp(Long accountId, String email, String otp) {
        sendOtp(
                accountId,
                email,
                "AUTH_FORGOT_PASSWORD_OTP",
                "FORGOT_PASSWORD_OTP",
                15,
                Map.of(
                        "user_name", fallbackName(null),
                        "otp_code", otp,
                        "expiry_minutes", 15));
    }

    public void sendChangeEmailOtp(
            Long accountId,
            String currentEmail,
            String newEmail,
            String otp) {
        sendOtp(
                accountId,
                currentEmail,
                "AUTH_CHANGE_EMAIL_OTP",
                "CHANGE_EMAIL_OTP",
                5,
                Map.of(
                        "user_name", fallbackName(null),
                        "new_email", newEmail,
                        "otp_code", otp,
                        "expiry_minutes", 5));
    }

    private void sendOtp(
            Long accountId,
            String email,
            String eventType,
            String templateKey,
            long expiryMinutes,
            Map<String, Object> payload) {
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotencyKey", eventType + "-" + eventId);
        body.put("sourceService", "auth-service");
        body.put("sourceEventId", eventId);
        body.put("eventType", eventType);
        body.put("templateKey", templateKey);
        body.put("locale", "vi-VN");
        body.put("category", "TRANSACTIONAL");
        body.put("priority", "HIGH");
        body.put("expiresAt", Instant.now().plus(expiryMinutes, ChronoUnit.MINUTES));
        body.put("test", false);
        body.put("recipient", Map.of(
                "userPublicId", String.valueOf(accountId),
                "email", email));
        body.put("channels", Set.of("EMAIL"));
        body.put("payload", payload);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Token", internalToken);
        try {
            restTemplate.postForEntity(
                    notificationServiceUrl + "/api/v1/internal/notifications",
                    new HttpEntity<>(body, headers),
                    Map.class);
        } catch (RestClientException exception) {
            throw new ExternalServiceUnavailableException("Notification Service", exception);
        }
    }

    private String fallbackName(String name) {
        return name == null || name.isBlank() ? "Khách hàng" : name.trim();
    }
}
