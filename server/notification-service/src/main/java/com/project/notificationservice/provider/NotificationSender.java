package com.project.notificationservice.provider;

import com.project.notificationservice.enums.NotificationChannel;
import com.project.notificationservice.provider.model.ProviderSendRequest;
import com.project.notificationservice.provider.model.ProviderSendResult;

public interface NotificationSender {
    NotificationChannel supportedChannel();
    ProviderSendResult send(ProviderSendRequest request);
}
