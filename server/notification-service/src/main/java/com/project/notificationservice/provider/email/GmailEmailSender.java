package com.project.notificationservice.provider.email;

import com.project.notificationservice.enums.NotificationChannel;
import com.project.notificationservice.provider.NotificationSender;
import com.project.notificationservice.provider.model.ProviderFailureCode;
import com.project.notificationservice.provider.model.ProviderSendRequest;
import com.project.notificationservice.provider.model.ProviderSendResult;
import com.project.notificationservice.provider.util.LogMaskingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.internet.MimeMessage;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.UUID;

@Component
public class GmailEmailSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(GmailEmailSender.class);
    
    private final JavaMailSender mailSender;

    public GmailEmailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public NotificationChannel supportedChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public ProviderSendResult send(ProviderSendRequest request) {
        String maskedRecipient = LogMaskingUtils.maskRecipient(request.getRecipient());
        log.info("Sending Gmail SMTP email: notificationId={}, channelType={}, providerName={}, recipient={}",
                request.getNotificationId(), request.getChannelType(), "GMAIL_SMTP", maskedRecipient);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(request.getRecipient());
            helper.setSubject(request.getTitle());
            helper.setText(request.getContent(), true);

            mailSender.send(message);

            String providerMessageId = UUID.randomUUID().toString();
            log.info("Gmail SMTP email sent successfully: notificationId={}, providerMessageId={}",
                    request.getNotificationId(), providerMessageId);

            return ProviderSendResult.builder()
                    .success(true)
                    .providerName("GMAIL_SMTP")
                    .providerMessageId(providerMessageId)
                    .build();

        } catch (Exception e) {
            ProviderFailureCode failure = classifyException(e);
            String sanitizedErrorMessage = sanitizeErrorMessage(e, failure);

            log.error("Gmail SMTP email failed: notificationId={}, failureCode={}, errorMessage={}",
                    request.getNotificationId(), failure.name(), sanitizedErrorMessage);

            return ProviderSendResult.builder()
                    .success(false)
                    .providerName("GMAIL_SMTP")
                    .failureCode(failure.name())
                    .errorMessage(sanitizedErrorMessage)
                    .retryable(failure.isRetryable())
                    .build();
        }
    }

    private ProviderFailureCode classifyException(Throwable e) {
        if (e instanceof MailAuthenticationException) {
            return ProviderFailureCode.PROVIDER_AUTH_FAILED;
        }
        if (e instanceof MailParseException || e instanceof MailPreparationException) {
            return ProviderFailureCode.INVALID_RECIPIENT;
        }

        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof SocketTimeoutException) {
                return ProviderFailureCode.PROVIDER_TIMEOUT;
            }
            if (cause instanceof ConnectException || cause instanceof UnknownHostException) {
                return ProviderFailureCode.PROVIDER_CONNECTION_FAILED;
            }
            if (cause instanceof SendFailedException || cause instanceof MessagingException) {
                String msg = cause.getMessage();
                if (msg != null) {
                    String lowerMsg = msg.toLowerCase();
                    if (lowerMsg.contains("authentication failed") || lowerMsg.contains("535")) {
                        return ProviderFailureCode.PROVIDER_AUTH_FAILED;
                    }
                    if (lowerMsg.contains("invalid address") || lowerMsg.contains("recipient rejected") || lowerMsg.contains("550")) {
                        return ProviderFailureCode.INVALID_RECIPIENT;
                    }
                    if (lowerMsg.contains("rate limit") || lowerMsg.contains("421")) {
                        return ProviderFailureCode.PROVIDER_RATE_LIMITED;
                    }
                }
            }
            cause = cause.getCause();
        }

        if (e instanceof MailSendException) {
            return ProviderFailureCode.PROVIDER_REJECTED;
        }

        return ProviderFailureCode.PROVIDER_UNAVAILABLE;
    }

    private String sanitizeErrorMessage(Throwable e, ProviderFailureCode failureCode) {
        switch (failureCode) {
            case PROVIDER_TIMEOUT:
                return "SMTP connection timed out.";
            case PROVIDER_CONNECTION_FAILED:
                return "Failed to establish a connection to the SMTP server.";
            case PROVIDER_RATE_LIMITED:
                return "Rate limit exceeded for SMTP provider.";
            case PROVIDER_AUTH_FAILED:
                return "SMTP authentication failed. Verify credentials.";
            case INVALID_RECIPIENT:
                return "Invalid recipient address or failed to parse recipient.";
            case PROVIDER_REJECTED:
                return "SMTP server rejected the transmission.";
            case PROVIDER_UNAVAILABLE:
            default:
                return "SMTP service is currently unavailable: " + (e.getMessage() != null ? e.getMessage() : "Unknown SMTP error");
        }
    }
}
