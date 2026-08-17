package com.project.notificationservice.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.notificationservice.domain.NotificationTypes.Channel;
import com.project.notificationservice.entity.NotificationAuditLog;
import com.project.notificationservice.repository.NotificationAuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@EnabledIfSystemProperty(named = "notification.template.remote-uri", matches = "https://.+")
class JGitRemoteTemplateSmokeTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void clonesAndRendersConfiguredLegacyRepository() {
        NotificationAuditLogRepository auditRepository = mock(NotificationAuditLogRepository.class);
        when(auditRepository.save(any(NotificationAuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        @SuppressWarnings("unchecked")
        ObjectProvider<StringRedisTemplate> redisProvider = mock(ObjectProvider.class);
        when(redisProvider.getIfAvailable()).thenReturn(null);
        SafeTemplateRenderer renderer = new SafeTemplateRenderer(500_000, 200, 480);
        JGitTemplateRegistry registry = new JGitTemplateRegistry(
                new ObjectMapper().findAndRegisterModules(),
                renderer,
                auditRepository,
                redisProvider,
                temporaryDirectory.resolve("registry").toString(),
                System.getProperty("notification.template.remote-uri"),
                "main",
                "",
                "",
                "Test",
                "test@example.com",
                200_000,
                15,
                false);
        try {
            registry.initialize();

            TemplateRegistry.TemplateDocument email = registry.getPublishedTemplate(
                    "BOOKING_CONFIRMED", Channel.EMAIL, "vi-VN");
            Map<String, Object> payload = Map.of(
                    "customerName", "An",
                    "bookingCode", "BK-001",
                    "movieTitle", "Lora Movie",
                    "cinemaName", "Lora Cinema",
                    "auditoriumName", "Hall 1",
                    "showtime", "2026-07-30T19:30:00+07:00",
                    "seatNames", List.of("A1", "A2"),
                    "totalAmount", 200_000,
                    "ticketAccessUrl", "https://example.com/tickets/one");
            TemplatePayloadAdapter adapter = new TemplatePayloadAdapter();

            assertThat(registry.health().available()).isTrue();
            assertThat(registry.findTemplates(null).size()).isGreaterThan(30);
            assertThat(renderer.render(email, adapter.adapt(payload, email)).htmlContent())
                    .contains("BK-001", "Lora Movie", "A1, A2");

            TemplateRegistry.TemplateDocument verifyEmail = registry.getPublishedTemplate(
                    "VERIFY_EMAIL", Channel.EMAIL, "vi-VN");
            assertThat(renderer.render(verifyEmail, adapter.adapt(Map.of(
                    "user_name", "An",
                    "verification_link", "https://example.com/verify",
                    "expiry_hours", 1), verifyEmail)).htmlContent())
                    .contains("An", "https://example.com/verify", "1");

            TemplateRegistry.TemplateDocument forgotPasswordOtp = registry.getPublishedTemplate(
                    "FORGOT_PASSWORD_OTP", Channel.EMAIL, "vi-VN");
            assertThat(renderer.render(forgotPasswordOtp, adapter.adapt(Map.of(
                    "user_name", "An",
                    "otp_code", "234567",
                    "expiry_minutes", 15), forgotPasswordOtp)).htmlContent())
                    .contains("An", "234567", "15");

            TemplateRegistry.TemplateDocument changeEmailOtp = registry.getPublishedTemplate(
                    "CHANGE_EMAIL_OTP", Channel.EMAIL, "vi-VN");
            assertThat(renderer.render(changeEmailOtp, adapter.adapt(Map.of(
                    "user_name", "An",
                    "new_email", "new@example.com",
                    "otp_code", "345678",
                    "expiry_minutes", 5), changeEmailOtp)).htmlContent())
                    .contains("An", "new&#64;example.com", "345678", "5");

            TemplateRegistry.TemplateDocument accountLocked = registry.getPublishedTemplate(
                    "ACCOUNT_LOCKED", Channel.EMAIL, "vi-VN");
            assertThat(accountLocked.sampleData())
                    .containsKeys("user_name", "lock_reason", "lock_time", "support_email");
            assertThat(registry.previewPublished(
                    "ACCOUNT_LOCKED", Channel.EMAIL, "vi-VN", null).rendered().htmlContent())
                    .contains("Nguyễn Minh Anh", "hotro&#64;lorafilm.vn");

            TemplateRegistry.TemplateDocument inApp = registry.getPublishedTemplate(
                    "BOOKING_CONFIRMED", Channel.IN_APP, "vi-VN");
            assertThat(renderer.render(inApp, adapter.adapt(payload, inApp)).textContent())
                    .contains("An", "LoraFilm Cinema");

            TemplateRegistry.TemplateDocument fallback = registry.getPublishedTemplate(
                    "PROMOTION_EVENT", Channel.WEB_PUSH, "vi-VN");
            assertThat(fallback.locale()).isEqualTo("vi-VN");
            assertThat(fallback.subject()).contains("{{event_title}}");
        } finally {
            registry.close();
        }
    }
}
