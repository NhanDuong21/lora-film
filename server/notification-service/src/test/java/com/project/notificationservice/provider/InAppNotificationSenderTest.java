package com.project.notificationservice.provider;

import com.project.notificationservice.entity.InAppNotification;
import com.project.notificationservice.repository.InAppNotificationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InAppNotificationSenderTest {

    @Test
    void persistsRichInboxNotificationWithRequestExpiry() {
        InAppNotificationRepository repository = mock(InAppNotificationRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> {
            InAppNotification item = invocation.getArgument(0);
            ReflectionTestUtils.setField(item, "publicId", "in-app-1");
            return item;
        });
        Instant expiresAt = Instant.parse("2026-08-06T01:00:00Z");
        NotificationChannelSender.RenderedNotification notification =
                new NotificationChannelSender.RenderedNotification(
                        "request-1", "delivery-1", "42", "42",
                        "Vé của bạn đã sẵn sàng", "<p>Vé</p>", "Mã đặt vé BK-001",
                        "/bookings/11111111-1111-4111-8111-111111111111",
                        "TRANSACTIONAL", expiresAt,
                        Map.of("_deliveryDatabaseId", 10L));

        var result = new InAppNotificationSender(repository).send(notification);

        ArgumentCaptor<InAppNotification> captor =
                ArgumentCaptor.forClass(InAppNotification.class);
        verify(repository).save(captor.capture());
        assertThat(result.successful()).isTrue();
        assertThat(captor.getValue().getUserPublicId()).isEqualTo("42");
        assertThat(captor.getValue().getExpiresAt()).isEqualTo(expiresAt);
        assertThat(captor.getValue().getDeepLink())
                .isEqualTo("/bookings/11111111-1111-4111-8111-111111111111");
    }
}
