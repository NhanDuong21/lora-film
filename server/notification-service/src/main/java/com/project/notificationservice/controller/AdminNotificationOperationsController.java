package com.project.notificationservice.controller;

import com.project.notificationservice.api.ApiResponse;
import com.project.notificationservice.domain.NotificationTypes.DeliveryStatus;
import com.project.notificationservice.entity.NotificationDeadLetter;
import com.project.notificationservice.entity.NotificationRequest;
import com.project.notificationservice.repository.NotificationDeadLetterRepository;
import com.project.notificationservice.repository.NotificationDeliveryRepository;
import com.project.notificationservice.repository.NotificationRequestRepository;
import com.project.notificationservice.service.NotificationApplicationService;
import com.project.notificationservice.template.TemplateRegistry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/notifications")
public class AdminNotificationOperationsController {

    private final NotificationRequestRepository requestRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationDeadLetterRepository deadLetterRepository;
    private final NotificationApplicationService service;
    private final TemplateRegistry templateRegistry;

    public AdminNotificationOperationsController(
            NotificationRequestRepository requestRepository,
            NotificationDeliveryRepository deliveryRepository,
            NotificationDeadLetterRepository deadLetterRepository,
            NotificationApplicationService service,
            TemplateRegistry templateRegistry) {
        this.requestRepository = requestRepository;
        this.deliveryRepository = deliveryRepository;
        this.deadLetterRepository = deadLetterRepository;
        this.service = service;
        this.templateRegistry = templateRegistry;
    }

    @GetMapping("/dashboard")
    public ApiResponse<Dashboard> dashboard() {
        EnumMap<DeliveryStatus, Long> counts = new EnumMap<>(DeliveryStatus.class);
        for (DeliveryStatus status : DeliveryStatus.values()) {
            counts.put(status, deliveryRepository.countByStatus(status));
        }
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        long delivered = counts.get(DeliveryStatus.DELIVERED) + counts.get(DeliveryStatus.SENT);
        long failures = counts.get(DeliveryStatus.FAILED) + counts.get(DeliveryStatus.DEAD_LETTERED);
        double rate = total == 0 ? 0 : delivered * 100.0 / total;
        return ApiResponse.success(new Dashboard(
                requestRepository.count(),
                total,
                delivered,
                failures,
                counts.get(DeliveryStatus.PENDING) + counts.get(DeliveryStatus.RETRY_SCHEDULED),
                counts.get(DeliveryStatus.DEAD_LETTERED),
                Math.round(rate * 100.0) / 100.0,
                Map.copyOf(counts),
                templateRegistry.health(),
                Instant.now()));
    }

    @GetMapping
    public ApiResponse<Page<RequestSummary>> requests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        Page<NotificationRequest> requests = requestRepository.findAll(PageRequest.of(
                Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt")));
        return ApiResponse.success(requests.map(request -> new RequestSummary(
                request.getPublicId(), request.getSourceService(), request.getSourceEventId(),
                request.getEventType(), request.getCorrelationId(), request.getTemplateKey(),
                request.getTemplateCommitSha(), request.getTemplateVersion(), request.getLocale(),
                request.getCategory().name(), request.getPriority().name(), request.getStatus().name(),
                request.isTest(), request.getCreatedAt())));
    }

    @GetMapping("/{publicId}")
    public ApiResponse<NotificationApplicationService.RequestDetails> request(
            @PathVariable String publicId) {
        return ApiResponse.success(service.get(publicId));
    }

    @PostMapping("/deliveries/{deliveryPublicId}/retry")
    public ApiResponse<NotificationApplicationService.DeliveryDetails> retry(
            @PathVariable String deliveryPublicId) {
        return ApiResponse.success(service.retryDelivery(deliveryPublicId));
    }

    @GetMapping("/dead-letters")
    public ApiResponse<Page<NotificationDeadLetter>> deadLetters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return ApiResponse.success(deadLetterRepository.findAll(PageRequest.of(
                Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    public record Dashboard(
            long totalRequests,
            long totalDeliveries,
            long delivered,
            long failed,
            long pending,
            long deadLetters,
            double deliveryRate,
            Map<DeliveryStatus, Long> deliveryStatuses,
            TemplateRegistry.RegistryHealth templateRegistry,
            Instant generatedAt) {
    }

    public record RequestSummary(
            String publicId,
            String sourceService,
            String sourceEventId,
            String eventType,
            String correlationId,
            String templateKey,
            String templateCommitSha,
            String templateVersion,
            String locale,
            String category,
            String priority,
            String status,
            boolean test,
            Instant createdAt) {
    }
}
