package com.project.notificationservice.provider;

import com.project.notificationservice.domain.NotificationTypes.Channel;
import com.project.notificationservice.domain.NotificationTypes.FailureCategory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MockWebPushNotificationSender implements NotificationChannelSender {

    @Override
    public Channel supportedChannel() {
        return Channel.WEB_PUSH;
    }

    @Override
    public DeliveryResult send(RenderedNotification notification) {
        if (notification.destination() == null || notification.destination().isBlank()) {
            return DeliveryResult.failure("mock-web-push", FailureCategory.INVALID_RECIPIENT,
                    "PUSH_SUBSCRIPTION_REQUIRED", "Web-push subscription is missing", null);
        }
        return DeliveryResult.success("mock-web-push", "push-" + UUID.randomUUID());
    }
}
