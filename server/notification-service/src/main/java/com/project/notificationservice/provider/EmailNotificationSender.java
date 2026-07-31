package com.project.notificationservice.provider;

import com.project.notificationservice.domain.NotificationTypes.Channel;
import com.project.notificationservice.domain.NotificationTypes.FailureCategory;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class EmailNotificationSender implements NotificationChannelSender {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String fromName;

    public EmailNotificationSender(
            JavaMailSender mailSender,
            @Value("${notification.delivery.from-address:notifications@lorafilm.local}") String fromAddress,
            @Value("${notification.delivery.from-name:LoraFilm}") String fromName) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
    }

    @Override
    public Channel supportedChannel() {
        return Channel.EMAIL;
    }

    @Override
    public DeliveryResult send(RenderedNotification notification) {
        if (notification.destination() == null || !notification.destination()
                .matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return DeliveryResult.failure("smtp", FailureCategory.INVALID_RECIPIENT,
                    "INVALID_EMAIL", "Email destination is invalid", null);
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress, fromName);
            helper.setTo(notification.destination());
            helper.setSubject(notification.subject());
            helper.setText(notification.textContent(), notification.htmlContent());
            message.setHeader("X-LoraFilm-Notification-Id", notification.notificationPublicId());
            mailSender.send(message);
            String providerId = message.getMessageID();
            return DeliveryResult.success("smtp",
                    providerId == null ? "smtp-" + UUID.randomUUID() : providerId);
        } catch (MailAuthenticationException exception) {
            return DeliveryResult.failure("smtp", FailureCategory.AUTHENTICATION_ERROR,
                    "SMTP_AUTHENTICATION_FAILED", "Email provider authentication failed", null);
        } catch (MailSendException exception) {
            return DeliveryResult.failure("smtp", FailureCategory.TRANSIENT,
                    "SMTP_SEND_FAILED", "Email provider is temporarily unavailable", null);
        } catch (Exception exception) {
            return DeliveryResult.failure("smtp", FailureCategory.PROVIDER_REJECTED,
                    "SMTP_REJECTED", "Email provider rejected the message", null);
        }
    }
}
