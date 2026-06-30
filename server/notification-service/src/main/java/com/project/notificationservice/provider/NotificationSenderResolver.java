package com.project.notificationservice.provider;

import com.project.notificationservice.enums.NotificationChannel;
import com.project.notificationservice.provider.config.NotificationProviderProperties;
import com.project.notificationservice.provider.email.GmailEmailSender;
import com.project.notificationservice.provider.email.MockEmailSender;
import com.project.notificationservice.provider.inapp.InAppNotificationSender;
import org.springframework.stereotype.Component;

@Component
public class NotificationSenderResolver {

    private final MockEmailSender mockEmailSender;
    private final GmailEmailSender gmailEmailSender;
    private final InAppNotificationSender inAppNotificationSender;
    private final NotificationProviderProperties properties;

    public NotificationSenderResolver(MockEmailSender mockEmailSender,
                                      GmailEmailSender gmailEmailSender,
                                      InAppNotificationSender inAppNotificationSender,
                                      NotificationProviderProperties properties) {
        this.mockEmailSender = mockEmailSender;
        this.gmailEmailSender = gmailEmailSender;
        this.inAppNotificationSender = inAppNotificationSender;
        this.properties = properties;
    }

    public NotificationSender resolve(NotificationChannel channel) {
        if (channel == null) {
            return null;
        }
        return switch (channel) {
            case EMAIL -> "gmail".equalsIgnoreCase(properties.getEmail().getProvider()) ? gmailEmailSender : mockEmailSender;
            case IN_APP -> inAppNotificationSender;
            default -> null;
        };
    }
}
