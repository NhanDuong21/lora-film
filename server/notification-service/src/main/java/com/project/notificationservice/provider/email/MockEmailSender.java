package com.project.notificationservice.provider.email;

import com.project.notificationservice.enums.NotificationChannel;
import com.project.notificationservice.provider.NotificationSender;
import com.project.notificationservice.provider.config.NotificationProviderProperties;
import com.project.notificationservice.provider.model.ProviderSendRequest;
import com.project.notificationservice.provider.model.ProviderSendResult;
import com.project.notificationservice.provider.util.LogMaskingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MockEmailSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(MockEmailSender.class);
    private final NotificationProviderProperties properties;

    public MockEmailSender(NotificationProviderProperties properties) {
        this.properties = properties;
    }

    @Override
    public NotificationChannel supportedChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public ProviderSendResult send(ProviderSendRequest request) {
        String maskedRecipient = LogMaskingUtils.maskRecipient(request.getRecipient());
        log.info("Sending mock email: notificationId={}, channelType={}, providerName={}, recipient={}",
                request.getNotificationId(), request.getChannelType(), "MOCK_EMAIL", maskedRecipient);

        if (properties.getMockEmail().isSimulateFailure()) {
            String failureCode = properties.getMockEmail().getFailureCode();
            boolean retryable = properties.getMockEmail().isRetryable();
            log.warn("Mock email simulated failure: notificationId={}, failureCode={}, retryable={}",
                    request.getNotificationId(), failureCode, retryable);

            return ProviderSendResult.builder()
                    .success(false)
                    .providerName("MOCK_EMAIL")
                    .providerMessageId(null)
                    .failureCode(failureCode)
                    .errorMessage("Simulated mock failure")
                    .retryable(retryable)
                    .build();
        }

        String providerMessageId = UUID.randomUUID().toString();
        log.info("Mock email sent successfully: notificationId={}, providerMessageId={}",
                request.getNotificationId(), providerMessageId);

        return ProviderSendResult.builder()
                .success(true)
                .providerName("MOCK_EMAIL")
                .providerMessageId(providerMessageId)
                .failureCode(null)
                .errorMessage(null)
                .retryable(false)
                .build();
    }
}
