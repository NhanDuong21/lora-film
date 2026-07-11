package com.project.notificationservice.provider.inapp;

import com.project.notificationservice.enums.NotificationChannel;
import com.project.notificationservice.provider.NotificationSender;
import com.project.notificationservice.provider.model.ProviderFailureCode;
import com.project.notificationservice.provider.model.ProviderSendRequest;
import com.project.notificationservice.provider.model.ProviderSendResult;
import com.project.notificationservice.provider.util.LogMaskingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class InAppNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(InAppNotificationSender.class);

    @Override
    public NotificationChannel supportedChannel() {
        return NotificationChannel.IN_APP;
    }

    @Override
    public ProviderSendResult send(ProviderSendRequest request) {
        String maskedRecipient = LogMaskingUtils.maskRecipient(request.getRecipient());
        log.info("Sending In-App notification: notificationId={}, channelType={}, providerName={}, recipient={}",
                request.getNotificationId(), request.getChannelType(), "INTERNAL", maskedRecipient);

        if (request.getUserId() == null) {
            log.error("In-App notification failed: userId is required");
            return ProviderSendResult.builder()
                    .success(false)
                    .providerName("INTERNAL")
                    .providerMessageId(null)
                    .failureCode(ProviderFailureCode.INVALID_RECIPIENT.name())
                    .errorMessage("userId is required for IN_APP notifications")
                    .retryable(false)
                    .build();
        }

        String providerMessageId = UUID.randomUUID().toString();
        log.info("In-App notification prepared successfully: notificationId={}, providerMessageId={}",
                request.getNotificationId(), providerMessageId);

        return ProviderSendResult.builder()
                .success(true)
                .providerName("INTERNAL")
                .providerMessageId(providerMessageId)
                .failureCode(null)
                .errorMessage(null)
                .retryable(false)
                .build();
    }
}
