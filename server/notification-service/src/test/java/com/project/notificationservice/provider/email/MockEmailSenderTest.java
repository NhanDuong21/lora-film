package com.project.notificationservice.provider.email;

import com.project.notificationservice.enums.NotificationChannel;
import com.project.notificationservice.provider.config.NotificationProviderProperties;
import com.project.notificationservice.provider.model.ProviderSendRequest;
import com.project.notificationservice.provider.model.ProviderSendResult;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MockEmailSenderTest {

    @Test
    public void testSend_Success() {
        NotificationProviderProperties properties = new NotificationProviderProperties();
        properties.getMockEmail().setSimulateFailure(false);

        MockEmailSender sender = new MockEmailSender(properties);

        ProviderSendRequest request = ProviderSendRequest.builder()
                .notificationId("123e4567-e89b-12d3-a456-426614174000")
                .channelType(NotificationChannel.EMAIL)
                .recipient("test@example.com")
                .title("Hello")
                .content("World")
                .build();

        ProviderSendResult result = sender.send(request);

        assertTrue(result.isSuccess());
        assertEquals("MOCK_EMAIL", result.getProviderName());
        assertNotNull(result.getProviderMessageId());
        assertNull(result.getFailureCode());
        assertNull(result.getErrorMessage());
        assertFalse(result.isRetryable());
    }

    @Test
    public void testSend_SimulatedRetryableFailure() {
        NotificationProviderProperties properties = new NotificationProviderProperties();
        properties.getMockEmail().setSimulateFailure(true);
        properties.getMockEmail().setFailureCode("PROVIDER_TIMEOUT");
        properties.getMockEmail().setRetryable(true);

        MockEmailSender sender = new MockEmailSender(properties);

        ProviderSendRequest request = ProviderSendRequest.builder()
                .notificationId("123e4567-e89b-12d3-a456-426614174000")
                .channelType(NotificationChannel.EMAIL)
                .recipient("test@example.com")
                .title("Hello")
                .content("World")
                .build();

        ProviderSendResult result = sender.send(request);

        assertFalse(result.isSuccess());
        assertEquals("MOCK_EMAIL", result.getProviderName());
        assertNull(result.getProviderMessageId());
        assertEquals("PROVIDER_TIMEOUT", result.getFailureCode());
        assertTrue(result.isRetryable());
    }

    @Test
    public void testSend_SimulatedNonRetryableFailure() {
        NotificationProviderProperties properties = new NotificationProviderProperties();
        properties.getMockEmail().setSimulateFailure(true);
        properties.getMockEmail().setFailureCode("INVALID_RECIPIENT");
        properties.getMockEmail().setRetryable(false);

        MockEmailSender sender = new MockEmailSender(properties);

        ProviderSendRequest request = ProviderSendRequest.builder()
                .notificationId("123e4567-e89b-12d3-a456-426614174000")
                .channelType(NotificationChannel.EMAIL)
                .recipient("test@example.com")
                .title("Hello")
                .content("World")
                .build();

        ProviderSendResult result = sender.send(request);

        assertFalse(result.isSuccess());
        assertEquals("MOCK_EMAIL", result.getProviderName());
        assertNull(result.getProviderMessageId());
        assertEquals("INVALID_RECIPIENT", result.getFailureCode());
        assertFalse(result.isRetryable());
    }
}
