package com.project.notificationservice.provider.email;

import com.project.notificationservice.enums.NotificationChannel;
import com.project.notificationservice.provider.model.ProviderFailureCode;
import com.project.notificationservice.provider.model.ProviderSendRequest;
import com.project.notificationservice.provider.model.ProviderSendResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GmailEmailSenderTest {

    private JavaMailSender mailSender;
    private GmailEmailSender gmailEmailSender;
    private MimeMessage mimeMessage;

    @BeforeEach
    public void setUp() {
        mailSender = mock(JavaMailSender.class);
        gmailEmailSender = new GmailEmailSender(mailSender);
        mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    public void testSend_Success() throws Exception {
        ProviderSendRequest request = ProviderSendRequest.builder()
                .notificationId("123e4567-e89b-12d3-a456-426614174000")
                .channelType(NotificationChannel.EMAIL)
                .recipient("john.doe@gmail.com")
                .title("Hi")
                .content("Mock body")
                .build();

        doNothing().when(mailSender).send(any(MimeMessage.class));

        ProviderSendResult result = gmailEmailSender.send(request);

        assertTrue(result.isSuccess());
        assertEquals("GMAIL_SMTP", result.getProviderName());
        assertNotNull(result.getProviderMessageId());
        assertNull(result.getFailureCode());
        assertNull(result.getErrorMessage());
        assertFalse(result.isRetryable());

        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    public void testSend_AuthFailure() throws Exception {
        ProviderSendRequest request = ProviderSendRequest.builder()
                .notificationId("123e4567-e89b-12d3-a456-426614174000")
                .channelType(NotificationChannel.EMAIL)
                .recipient("john.doe@gmail.com")
                .title("Hi")
                .content("Mock body")
                .build();

        doThrow(new MailAuthenticationException("Auth failed")).when(mailSender).send(any(MimeMessage.class));

        ProviderSendResult result = gmailEmailSender.send(request);

        assertFalse(result.isSuccess());
        assertEquals("GMAIL_SMTP", result.getProviderName());
        assertEquals(ProviderFailureCode.PROVIDER_AUTH_FAILED.name(), result.getFailureCode());
        assertFalse(result.isRetryable());
        assertTrue(result.getErrorMessage().contains("authentication failed"));
    }

    @Test
    public void testSend_ConnectionFailure() throws Exception {
        ProviderSendRequest request = ProviderSendRequest.builder()
                .notificationId("123e4567-e89b-12d3-a456-426614174000")
                .channelType(NotificationChannel.EMAIL)
                .recipient("john.doe@gmail.com")
                .title("Hi")
                .content("Mock body")
                .build();

        MailSendException mailSendEx = new MailSendException("Mail send failed",
                new ConnectException("Connection refused"));
        doThrow(mailSendEx).when(mailSender).send(any(MimeMessage.class));

        ProviderSendResult result = gmailEmailSender.send(request);

        assertFalse(result.isSuccess());
        assertEquals(ProviderFailureCode.PROVIDER_CONNECTION_FAILED.name(), result.getFailureCode());
        assertTrue(result.isRetryable());
        assertTrue(result.getErrorMessage().contains("establish a connection"));
    }

    @Test
    public void testSend_TimeoutFailure() throws Exception {
        ProviderSendRequest request = ProviderSendRequest.builder()
                .notificationId("123e4567-e89b-12d3-a456-426614174000")
                .channelType(NotificationChannel.EMAIL)
                .recipient("hoangtlt.ce190272@gmail.com")
                .title("Hi")
                .content("Mock body")
                .build();

        MailSendException mailSendEx = new MailSendException("Mail send failed",
                new SocketTimeoutException("Read timed out"));
        doThrow(mailSendEx).when(mailSender).send(any(MimeMessage.class));

        ProviderSendResult result = gmailEmailSender.send(request);

        assertFalse(result.isSuccess());
        assertEquals(ProviderFailureCode.PROVIDER_TIMEOUT.name(), result.getFailureCode());
        assertTrue(result.isRetryable());
        assertTrue(result.getErrorMessage().contains("timed out"));
    }

    @Test
    public void testSend_ParseOrRecipientFailure() throws Exception {
        ProviderSendRequest request = ProviderSendRequest.builder()
                .notificationId("123e4567-e89b-12d3-a456-426614174000")
                .channelType(NotificationChannel.EMAIL)
                .recipient("invalid-email")
                .title("Hi")
                .content("Mock body")
                .build();

        doThrow(new MailParseException("Parse failed")).when(mailSender).send(any(MimeMessage.class));

        ProviderSendResult result = gmailEmailSender.send(request);

        assertFalse(result.isSuccess());
        assertEquals(ProviderFailureCode.INVALID_RECIPIENT.name(), result.getFailureCode());
        assertFalse(result.isRetryable());
    }
}
