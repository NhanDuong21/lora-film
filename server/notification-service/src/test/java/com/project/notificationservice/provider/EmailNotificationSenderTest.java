package com.project.notificationservice.provider;

import com.project.notificationservice.domain.NotificationTypes.FailureCategory;
import com.project.notificationservice.provider.NotificationChannelSender.DeliveryResult;
import com.project.notificationservice.provider.NotificationChannelSender.RenderedNotification;
import com.project.notificationservice.service.EmailProviderConfigurationService;
import com.project.notificationservice.service.EmailProviderConfigurationService.ActiveEmailSender;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.eclipse.angus.mail.smtp.SMTPAddressFailedException;
import org.eclipse.angus.mail.smtp.SMTPSendFailedException;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmailNotificationSenderTest {

    @Test
    void reportsAuthenticationFailureSeparately() {
        DeliveryResult result = sendWith(new MailAuthenticationException("invalid credentials"));

        assertThat(result.failureCategory()).isEqualTo(FailureCategory.AUTHENTICATION_ERROR);
        assertThat(result.failureCode()).isEqualTo("SMTP_AUTHENTICATION_FAILED");
    }

    @Test
    void reportsConnectionFailureSeparately() {
        DeliveryResult result = sendWith(new MailSendException(
                "send failed", new SocketTimeoutException("connection timed out")));

        assertThat(result.failureCategory()).isEqualTo(FailureCategory.TRANSIENT);
        assertThat(result.failureCode()).isEqualTo("SMTP_CONNECTION_FAILED");
    }

    @Test
    void reportsPermanentRecipientRejectionSeparately() throws Exception {
        SMTPAddressFailedException providerFailure = new SMTPAddressFailedException(
                new InternetAddress("recipient@example.com"), "RCPT TO", 550, "recipient rejected");
        DeliveryResult result = sendWith(new MailSendException("send failed", providerFailure));

        assertThat(result.failureCategory()).isEqualTo(FailureCategory.INVALID_RECIPIENT);
        assertThat(result.failureCode()).isEqualTo("SMTP_RECIPIENT_REJECTED");
    }

    @Test
    void reportsGmailPolicyRejectionSeparately() {
        SMTPSendFailedException providerFailure = new SMTPSendFailedException(
                ".", 550,
                "550-5.7.1 Message rejected under policy for customer@example.com",
                null, null, null, null);
        DeliveryResult result = sendWith(new MailSendException("send failed", providerFailure));

        assertThat(result.failureCategory()).isEqualTo(FailureCategory.PROVIDER_REJECTED);
        assertThat(result.failureCode()).isEqualTo("SMTP_POLICY_REJECTED");
        assertThat(result.failureMessage()).contains("[email]").doesNotContain("customer@example.com");
    }

    @Test
    void reportsSendingQuotaSeparately() {
        SMTPSendFailedException providerFailure = new SMTPSendFailedException(
                ".", 550, "550-5.4.5 Daily user sending limit exceeded", null, null, null, null);
        DeliveryResult result = sendWith(new MailSendException("send failed", providerFailure));

        assertThat(result.failureCode()).isEqualTo("SMTP_QUOTA_EXCEEDED");
    }

    private DeliveryResult sendWith(RuntimeException failure) {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doThrow(failure).when(mailSender).send(any(MimeMessage.class));
        EmailProviderConfigurationService configurationService = mock(EmailProviderConfigurationService.class);
        when(configurationService.currentSender()).thenReturn(new ActiveEmailSender(
                mailSender, "notifications@example.com", "LoraFilm", "TEST"));
        EmailNotificationSender sender = new EmailNotificationSender(configurationService);

        return sender.send(new RenderedNotification(
                "request-1", "delivery-1", "user-1", "recipient@example.com",
                "Mã xác thực", "<p>Mã xác thực</p>", "Mã xác thực", null,
                "SECURITY", null, Map.of()));
    }
}
