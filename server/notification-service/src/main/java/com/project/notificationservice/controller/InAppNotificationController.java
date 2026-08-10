package com.project.notificationservice.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.notificationservice.api.ApiResponse;
import com.project.notificationservice.entity.InAppNotification;
import com.project.notificationservice.entity.NotificationDelivery;
import com.project.notificationservice.entity.NotificationRequest;
import com.project.notificationservice.exception.NotificationException;
import com.project.notificationservice.repository.InAppNotificationRepository;
import com.project.notificationservice.repository.NotificationDeliveryRepository;
import com.project.notificationservice.repository.NotificationRequestRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/notifications")
public class InAppNotificationController {

    private static final Set<String> TICKET_DATA_FIELDS = Set.of(
            "bookingPublicId", "bookingCode", "paymentCode",
            "movieTitle", "moviePosterUrl", "movieAgeRating",
            "cinemaName", "auditoriumName", "showtime",
            "seatNames", "ticketCodes", "ticketTypes", "ticketPublicIds",
            "foodItems", "promotionCode", "promotionName",
            "subtotal", "discount", "totalPaid", "totalAmount", "currency",
            "paymentMethod", "paymentTime");

    private final InAppNotificationRepository repository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationRequestRepository requestRepository;
    private final ObjectMapper objectMapper;

    public InAppNotificationController(
            InAppNotificationRepository repository,
            NotificationDeliveryRepository deliveryRepository,
            NotificationRequestRepository requestRepository,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.deliveryRepository = deliveryRepository;
        this.requestRepository = requestRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ApiResponse<Page<InAppView>> list(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String owner = authentication.getName();
        Page<InAppNotification> items =
                repository.findByUserPublicIdAndExpiresAtAfterOrUserPublicIdAndExpiresAtIsNullOrderByCreatedAtDesc(
                        owner, Instant.now(), owner,
                        PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                                Sort.by(Sort.Direction.DESC, "createdAt")));
        return ApiResponse.success(items.map(this::view));
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCount> unread(Authentication authentication) {
        return ApiResponse.success(new UnreadCount(
                repository.countActiveUnread(authentication.getName(), Instant.now())));
    }

    @PatchMapping("/{publicId}/read")
    @Transactional
    public ApiResponse<InAppView> markRead(
            Authentication authentication,
            @PathVariable String publicId) {
        InAppNotification item = repository.findByPublicIdAndUserPublicId(
                        publicId, authentication.getName())
                .orElseThrow(() -> new NotificationException("IN_APP_NOTIFICATION_NOT_FOUND",
                        "Notification was not found for this user", HttpStatus.NOT_FOUND));
        if (item.getReadAt() == null) item.setReadAt(Instant.now());
        return ApiResponse.success(view(item));
    }

    @PatchMapping("/read-all")
    @Transactional
    public ApiResponse<UpdatedCount> markAllRead(Authentication authentication) {
        return ApiResponse.success(new UpdatedCount(
                repository.markAllRead(authentication.getName(), Instant.now())));
    }

    private InAppView view(InAppNotification item) {
        NotificationRequest request = deliveryRepository.findById(item.getNotificationDeliveryId())
                .map(NotificationDelivery::getNotificationRequestId)
                .flatMap(requestRepository::findById)
                .orElse(null);
        Map<String, Object> data = request == null ? Map.of() : publicData(request);
        return new InAppView(item.getPublicId(), item.getTitle(), item.getBody(),
                request == null ? "GENERIC" : request.getEventType(),
                item.getCategory(),
                request == null ? "NORMAL" : request.getPriority().name(),
                actionUrl(item, data), data, item.getReadAt(), item.getExpiresAt(),
                item.getCreatedAt());
    }

    private Map<String, Object> publicData(NotificationRequest request) {
        try {
            Map<String, Object> payload = objectMapper.readValue(
                    request.getPayloadJson(), new TypeReference<>() {
                    });
            Map<String, Object> result = new LinkedHashMap<>();
            if ("TICKET_PURCHASED".equals(request.getEventType())
                    || "TICKET_ISSUED".equals(request.getEventType())
                    || "BOOKING_CONFIRMED".equals(request.getEventType())) {
                TICKET_DATA_FIELDS.forEach(key -> {
                    if (payload.containsKey(key)) result.put(key, payload.get(key));
                });
            } else if (payload.get("publicData") instanceof Map<?, ?> publicValues) {
                publicValues.forEach((key, value) -> {
                    if (key instanceof String field && !field.startsWith("_")) {
                        result.put(field, value);
                    }
                });
            }
            return Collections.unmodifiableMap(result);
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private String actionUrl(InAppNotification item, Map<String, Object> data) {
        Object bookingPublicId = data.get("bookingPublicId");
        String bookingId = bookingPublicId == null ? "" : String.valueOf(bookingPublicId);
        if (bookingId.matches(
                "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-"
                        + "[a-fA-F0-9]{4}-[a-fA-F0-9]{12}")) {
            return "/bookings/" + bookingId;
        }
        return item.getDeepLink();
    }

    public record InAppView(
            String publicId,
            String title,
            String body,
            String notificationType,
            String category,
            String priority,
            String actionUrl,
            Map<String, Object> data,
            Instant readAt,
            Instant expiresAt,
            Instant createdAt) {
    }

    public record UnreadCount(long count) {
    }

    public record UpdatedCount(int count) {
    }
}
