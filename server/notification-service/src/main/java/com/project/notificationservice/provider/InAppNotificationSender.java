package com.project.notificationservice.provider;

import com.project.notificationservice.domain.NotificationTypes.Channel;
import com.project.notificationservice.domain.NotificationTypes.FailureCategory;
import com.project.notificationservice.entity.InAppNotification;
import com.project.notificationservice.repository.InAppNotificationRepository;
import org.springframework.stereotype.Component;

@Component
public class InAppNotificationSender implements NotificationChannelSender {

    private final InAppNotificationRepository repository;

    public InAppNotificationSender(InAppNotificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Channel supportedChannel() {
        return Channel.IN_APP;
    }

    @Override
    public DeliveryResult send(RenderedNotification notification) {
        if (notification.userPublicId() == null || notification.userPublicId().isBlank()) {
            return DeliveryResult.failure("in-app", FailureCategory.INVALID_RECIPIENT,
                    "USER_PUBLIC_ID_REQUIRED", "In-app delivery requires a user public identifier", null);
        }
        InAppNotification item = new InAppNotification();
        item.setNotificationDeliveryId(Long.parseLong(
                String.valueOf(notification.payload().get("_deliveryDatabaseId"))));
        item.setUserPublicId(notification.userPublicId());
        item.setTitle(notification.subject());
        item.setBody(notification.textContent());
        item.setCategory(notification.category());
        item.setDeepLink(notification.deepLink());
        InAppNotification saved = repository.save(item);
        return DeliveryResult.success("in-app", saved.getPublicId());
    }
}
