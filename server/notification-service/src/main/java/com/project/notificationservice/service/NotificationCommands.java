package com.project.notificationservice.service;

import com.project.notificationservice.domain.NotificationTypes.Category;
import com.project.notificationservice.domain.NotificationTypes.Channel;
import com.project.notificationservice.domain.NotificationTypes.Priority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

public final class NotificationCommands {

    private NotificationCommands() {
    }

    public record RecipientCommand(
            @Size(max = 80) String userPublicId,
            @Size(max = 320) String email,
            @Size(max = 30) String phone,
            @Size(max = 2000) String webPushSubscription) {
    }

    public record CreateNotificationCommand(
            @NotBlank @Size(max = 200) String idempotencyKey,
            @NotBlank @Size(max = 80) String sourceService,
            @Size(max = 80) String sourceEventId,
            @NotBlank @Size(max = 100) String eventType,
            @Size(max = 80) String correlationId,
            @Size(max = 80) String causationId,
            @NotBlank @Pattern(regexp = "[A-Z0-9_]{3,100}") String templateKey,
            @NotBlank @Pattern(regexp = "[a-z]{2}-[A-Z]{2}") String locale,
            @NotNull Category category,
            @NotNull Priority priority,
            Instant scheduledAt,
            Instant expiresAt,
            boolean test,
            @NotNull @Valid RecipientCommand recipient,
            @NotEmpty Set<Channel> channels,
            @NotNull @Size(max = 200) Map<String, Object> payload) {
    }

    public record AcceptedNotification(
            String publicId,
            String status,
            boolean idempotent,
            int deliveryCount) {
    }

    public record CouponIssuedNotification(
            String sourceEventId,
            String userPublicId,
            String couponCode,
            String promotionName,
            Instant expiresAt,
            String deepLink) {
    }
}
