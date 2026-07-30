package com.project.notificationservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.notificationservice.domain.NotificationTypes.Priority;
import com.project.notificationservice.entity.InAppNotification;
import com.project.notificationservice.entity.NotificationDelivery;
import com.project.notificationservice.entity.NotificationRequest;
import com.project.notificationservice.repository.InAppNotificationRepository;
import com.project.notificationservice.repository.NotificationDeliveryRepository;
import com.project.notificationservice.repository.NotificationRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InAppNotificationControllerTest {

    private InAppNotificationRepository inAppRepository;
    private NotificationDeliveryRepository deliveryRepository;
    private NotificationRequestRepository requestRepository;
    private InAppNotificationController controller;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        inAppRepository = mock(InAppNotificationRepository.class);
        deliveryRepository = mock(NotificationDeliveryRepository.class);
        requestRepository = mock(NotificationRequestRepository.class);
        controller = new InAppNotificationController(
                inAppRepository, deliveryRepository, requestRepository,
                new ObjectMapper().findAndRegisterModules());
        authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("42");
    }

    @Test
    void ticketNotificationReturnsStructuredTicketDataAndSafeBookingAction() {
        InAppNotification item = notification();
        NotificationDelivery delivery = new NotificationDelivery();
        ReflectionTestUtils.setField(delivery, "id", 10L);
        delivery.setNotificationRequestId(20L);
        NotificationRequest request = new NotificationRequest();
        ReflectionTestUtils.setField(request, "id", 20L);
        request.setEventType("TICKET_PURCHASED");
        request.setPriority(Priority.HIGH);
        request.setPayloadJson("""
                {
                  "bookingPublicId":"booking-123",
                  "bookingCode":"BK-001",
                  "movieTitle":"Mưa đỏ",
                  "seatNames":["A1","A2"],
                  "totalPaid":190000,
                  "email":"hidden@example.com",
                  "_deliveryDatabaseId":99
                }
                """);
        when(inAppRepository
                .findByUserPublicIdAndExpiresAtAfterOrUserPublicIdAndExpiresAtIsNullOrderByCreatedAtDesc(
                        anyString(), any(), anyString(), any()))
                .thenReturn(new PageImpl<>(List.of(item)));
        when(deliveryRepository.findById(10L)).thenReturn(Optional.of(delivery));
        when(requestRepository.findById(20L)).thenReturn(Optional.of(request));

        var response = controller.list(authentication, 0, 20);
        var view = response.data().getContent().getFirst();

        assertThat(view.notificationType()).isEqualTo("TICKET_PURCHASED");
        assertThat(view.priority()).isEqualTo("HIGH");
        assertThat(view.actionUrl()).isEqualTo("/bookings/booking-123");
        assertThat(view.data()).containsEntry("bookingCode", "BK-001")
                .containsEntry("movieTitle", "Mưa đỏ")
                .containsEntry("totalPaid", new BigDecimal("190000"));
        assertThat(view.data()).doesNotContainKeys("email", "_deliveryDatabaseId");
    }

    @Test
    void unreadCountUsesAuthenticatedAccountIdAndExcludesExpiredItems() {
        when(inAppRepository.countActiveUnread(anyString(), any())).thenReturn(3L);

        var response = controller.unread(authentication);

        assertThat(response.data().count()).isEqualTo(3L);
        verify(inAppRepository).countActiveUnread(anyString(), any(Instant.class));
    }

    private InAppNotification notification() {
        InAppNotification item = new InAppNotification();
        ReflectionTestUtils.setField(item, "publicId", "notification-1");
        ReflectionTestUtils.setField(item, "createdAt", Instant.parse("2026-07-30T01:00:00Z"));
        item.setNotificationDeliveryId(10L);
        item.setUserPublicId("42");
        item.setTitle("Vé của bạn đã sẵn sàng");
        item.setBody("Mã đặt vé BK-001");
        item.setCategory("TRANSACTIONAL");
        item.setDeepLink("https://untrusted.example/ticket");
        return item;
    }
}
