package com.project.notificationservice.provider.inapp;

import com.project.notificationservice.enums.NotificationChannel;
import com.project.notificationservice.provider.model.ProviderFailureCode;
import com.project.notificationservice.provider.model.ProviderSendRequest;
import com.project.notificationservice.provider.model.ProviderSendResult;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class InAppNotificationSenderTest {

    @Test
    public void testSend_Success() {
        InAppNotificationSender sender = new InAppNotificationSender();

        ProviderSendRequest request = ProviderSendRequest.builder()
                .notificationId("123e4567-e89b-12d3-a456-426614174000")
                .userId(100L)
                .channelType(NotificationChannel.IN_APP)
                .recipient(null)
                .title("Welcome")
                .content("Hello World")
                .build();

        ProviderSendResult result = sender.send(request);

        assertTrue(result.isSuccess());
        assertEquals("INTERNAL", result.getProviderName());
        assertNotNull(result.getProviderMessageId());
        assertNull(result.getFailureCode());
        assertNull(result.getErrorMessage());
        assertFalse(result.isRetryable());
    }

    @Test
    public void testSend_MissingUserId() {
        InAppNotificationSender sender = new InAppNotificationSender();

        ProviderSendRequest request = ProviderSendRequest.builder()
                .notificationId("123e4567-e89b-12d3-a456-426614174000")
                .userId(null)
                .channelType(NotificationChannel.IN_APP)
                .recipient(null)
                .title("Welcome")
                .content("Hello World")
                .build();

        ProviderSendResult result = sender.send(request);

        assertFalse(result.isSuccess());
        assertEquals("INTERNAL", result.getProviderName());
        assertNull(result.getProviderMessageId());
        assertEquals(ProviderFailureCode.INVALID_RECIPIENT.name(), result.getFailureCode());
        assertNotNull(result.getErrorMessage());
        assertFalse(result.isRetryable());
    }
}
