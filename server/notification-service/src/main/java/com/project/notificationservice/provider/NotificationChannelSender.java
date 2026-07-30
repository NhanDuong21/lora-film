package com.project.notificationservice.provider;

import com.project.notificationservice.domain.NotificationTypes.Channel;
import com.project.notificationservice.domain.NotificationTypes.FailureCategory;

import java.util.Map;

public interface NotificationChannelSender {

    Channel supportedChannel();

    DeliveryResult send(RenderedNotification notification);

    record RenderedNotification(
            String notificationPublicId,
            String deliveryPublicId,
            String userPublicId,
            String destination,
            String subject,
            String htmlContent,
            String textContent,
            String deepLink,
            String category,
            Map<String, Object> payload) {
    }

    record DeliveryResult(
            boolean successful,
            String provider,
            String providerMessageId,
            FailureCategory failureCategory,
            String failureCode,
            String failureMessage,
            Long retryAfterSeconds) {

        public static DeliveryResult success(String provider, String providerMessageId) {
            return new DeliveryResult(true, provider, providerMessageId, null, null, null, null);
        }

        public static DeliveryResult failure(
                String provider,
                FailureCategory category,
                String code,
                String message,
                Long retryAfterSeconds) {
            return new DeliveryResult(false, provider, null, category, code, message, retryAfterSeconds);
        }
    }
}
