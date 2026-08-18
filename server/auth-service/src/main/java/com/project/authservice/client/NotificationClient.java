package com.project.authservice.client;

import com.project.authservice.exception.OtpDeliveryFailedException;
import com.project.authservice.exception.OtpDeliveryPendingException;
import com.project.authservice.exception.common.ExternalServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class NotificationClient {

    private static final long DELIVERY_POLL_INTERVAL_MILLIS = 250L;
    private static final Set<String> SUCCESSFUL_DELIVERY_STATUSES = Set.of("SENT", "DELIVERED");
    private static final Set<String> FAILED_DELIVERY_STATUSES = Set.of(
            "FAILED", "DEAD_LETTERED", "CANCELLED", "SUPPRESSED");

    private final RestTemplate restTemplate;
    private final String notificationServiceUrl;
    private final String internalToken;
    private final long deliveryWaitMillis;

    public NotificationClient(
            RestTemplate restTemplate,
            @Value("${app.notification-service.url}") String notificationServiceUrl,
            @Value("${app.internal-token}") String internalToken,
            @Value("${app.notification-service.delivery-wait-ms:15000}") long deliveryWaitMillis) {
        this.restTemplate = restTemplate;
        this.notificationServiceUrl = notificationServiceUrl;
        this.internalToken = internalToken;
        this.deliveryWaitMillis = deliveryWaitMillis;
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
                        "email", email,
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
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    notificationServiceUrl + "/api/v1/internal/notifications",
                    new HttpEntity<>(body, headers),
                    Map.class);
            String notificationPublicId = acceptedNotificationPublicId(response.getBody());
            if (notificationPublicId == null) {
                throw new ExternalServiceUnavailableException(
                        "Notification Service returned an invalid acceptance response");
            }
            waitForEmailDelivery(notificationPublicId, headers);
        } catch (OtpDeliveryFailedException | OtpDeliveryPendingException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new ExternalServiceUnavailableException("Notification Service", exception);
        }
    }

    public void sendEmployeeInvitation(Long accountId, String email, String name, String otp) {
        sendOtp(
                accountId,
                email,
                "AUTH_EMPLOYEE_INVITATION",
                "FORGOT_PASSWORD_OTP",
                48 * 60,
                Map.of(
                        "user_name", name == null || name.isBlank() ? "Nhân viên" : name.trim(),
                        "otp_code", otp,
                        "email", email,
                        "expiry_minutes", 48 * 60));
    }

    private void waitForEmailDelivery(String notificationPublicId, HttpHeaders headers) {
        long deadline = System.nanoTime() + deliveryWaitMillis * 1_000_000L;
        while (System.nanoTime() < deadline) {
            ResponseEntity<Map> response = restTemplate.exchange(
                    notificationServiceUrl + "/api/v1/internal/notifications/"
                            + notificationPublicId + "/deliveries",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class);
            DeliveryState state = emailDeliveryState(response.getBody());
            if (state.successful()) {
                return;
            }
            if (state.failed()) {
                throw new OtpDeliveryFailedException(state.failureCode());
            }
            sleepBeforeNextPoll();
        }
        throw new OtpDeliveryPendingException();
    }

    private String acceptedNotificationPublicId(Map<?, ?> response) {
        Object data = response == null ? null : response.get("data");
        if (!(data instanceof Map<?, ?> accepted)) return null;
        Object publicId = accepted.get("publicId");
        return publicId instanceof String value && !value.isBlank() ? value : null;
    }

    private DeliveryState emailDeliveryState(Map<?, ?> response) {
        Object data = response == null ? null : response.get("data");
        if (!(data instanceof List<?> deliveries)) return DeliveryState.pending();
        for (Object value : deliveries) {
            if (!(value instanceof Map<?, ?> delivery)) continue;
            if (!"EMAIL".equalsIgnoreCase(stringValue(delivery.get("channel")))) continue;
            String status = stringValue(delivery.get("status")).toUpperCase(java.util.Locale.ROOT);
            if (SUCCESSFUL_DELIVERY_STATUSES.contains(status)) return DeliveryState.success();
            if (FAILED_DELIVERY_STATUSES.contains(status)) {
                return DeliveryState.failure(stringValue(delivery.get("failureCode")));
            }
        }
        return DeliveryState.pending();
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private void sleepBeforeNextPoll() {
        try {
            Thread.sleep(DELIVERY_POLL_INTERVAL_MILLIS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new OtpDeliveryPendingException();
        }
    }

    private record DeliveryState(boolean successful, boolean failed, String failureCode) {
        private static DeliveryState success() {
            return new DeliveryState(true, false, null);
        }

        private static DeliveryState failure(String failureCode) {
            return new DeliveryState(false, true,
                    failureCode == null || failureCode.isBlank() ? "UNKNOWN_FAILURE" : failureCode);
        }

        private static DeliveryState pending() {
            return new DeliveryState(false, false, null);
        }
    }

    private String fallbackName(String name) {
        return name == null || name.isBlank() ? "Khách hàng" : name.trim();
    }
}
