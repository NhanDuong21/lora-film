package com.project.notificationservice.provider;

import com.project.notificationservice.domain.NotificationTypes.Channel;
import com.project.notificationservice.domain.NotificationTypes.FailureCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MockSmsNotificationSender implements NotificationChannelSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockSmsNotificationSender.class);

    @Override
    public Channel supportedChannel() {
        return Channel.SMS;
    }

    @Override
    public DeliveryResult send(RenderedNotification notification) {
        if (notification.destination() == null || !notification.destination().matches("^\\+?[0-9]{8,15}$")) {
            return DeliveryResult.failure("mock-sms", FailureCategory.INVALID_RECIPIENT,
                    "INVALID_PHONE", "Phone destination is invalid", null);
        }
        LOGGER.info("Mock SMS accepted deliveryPublicId={} destinationSuffix={}",
                notification.deliveryPublicId(),
                notification.destination().substring(Math.max(0, notification.destination().length() - 4)));
        return DeliveryResult.success("mock-sms", "sms-" + UUID.randomUUID());
    }
}
