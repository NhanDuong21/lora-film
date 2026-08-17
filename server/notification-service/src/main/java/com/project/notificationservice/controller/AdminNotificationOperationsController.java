package com.project.notificationservice.controller;

import com.project.notificationservice.api.ApiResponse;
import com.project.notificationservice.domain.NotificationTypes.DeliveryStatus;
import com.project.notificationservice.domain.NotificationTypes.RequestStatus;
import com.project.notificationservice.entity.NotificationDeadLetter;
import com.project.notificationservice.entity.NotificationRequest;
import com.project.notificationservice.repository.NotificationDeadLetterRepository;
import com.project.notificationservice.repository.NotificationDeliveryRepository;
import com.project.notificationservice.repository.NotificationRequestRepository;
import com.project.notificationservice.service.NotificationApplicationService;
import com.project.notificationservice.template.TemplateRegistry;
import com.project.notificationservice.template.TemplateContractCoverageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/notifications")
public class AdminNotificationOperationsController {

    private final NotificationRequestRepository requestRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationDeadLetterRepository deadLetterRepository;
    private final NotificationApplicationService service;
    private final TemplateRegistry templateRegistry;
    private final TemplateContractCoverageService coverageService;

    public AdminNotificationOperationsController(
            NotificationRequestRepository requestRepository,
            NotificationDeliveryRepository deliveryRepository,
            NotificationDeadLetterRepository deadLetterRepository,
            NotificationApplicationService service,
            TemplateRegistry templateRegistry,
            TemplateContractCoverageService coverageService) {
        this.requestRepository = requestRepository;
        this.deliveryRepository = deliveryRepository;
        this.deadLetterRepository = deadLetterRepository;
        this.service = service;
        this.templateRegistry = templateRegistry;
        this.coverageService = coverageService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<Dashboard> dashboard(
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "false") boolean includeTest) {
        int safeHours = Math.min(Math.max(hours, 1), 24 * 90);
        Instant since = Instant.now().minus(Duration.ofHours(safeHours));
        EnumMap<DeliveryStatus, Long> counts = new EnumMap<>(DeliveryStatus.class);
        for (DeliveryStatus status : DeliveryStatus.values()) {
            counts.put(status, deliveryRepository.countOperationalByStatus(
                    status, since, includeTest));
        }
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        long accepted = counts.get(DeliveryStatus.DELIVERED) + counts.get(DeliveryStatus.SENT);
        long confirmed = counts.get(DeliveryStatus.DELIVERED);
        long failures = counts.get(DeliveryStatus.FAILED) + counts.get(DeliveryStatus.DEAD_LETTERED);
        double rate = total == 0 ? 0 : accepted * 100.0 / total;
        long requests = includeTest
                ? requestRepository.countByCreatedAtGreaterThanEqual(since)
                : requestRepository.countByTestFalseAndCreatedAtGreaterThanEqual(since);
        return ApiResponse.success(new Dashboard(
                requests,
                total,
                accepted,
                confirmed,
                accepted,
                failures,
                counts.get(DeliveryStatus.PENDING) + counts.get(DeliveryStatus.RETRY_SCHEDULED),
                counts.get(DeliveryStatus.DEAD_LETTERED),
                Math.round(rate * 100.0) / 100.0,
                Map.copyOf(counts),
                templateRegistry.health(),
                coverageService.inspect(),
                safeHours,
                includeTest,
                Instant.now()));
    }

    @GetMapping
    public ApiResponse<Page<RequestSummary>> requests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String sourceService,
            @RequestParam(required = false) String templateKey,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) Boolean test,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        Specification<NotificationRequest> specification = Specification
                .where((root, ignored, criteria) -> criteria.conjunction());
        if (query != null && !query.isBlank()) {
            String pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, ignored, criteria) -> criteria.or(
                    criteria.like(criteria.lower(root.get("publicId")), pattern),
                    criteria.like(criteria.lower(root.get("sourceEventId")), pattern),
                    criteria.like(criteria.lower(root.get("eventType")), pattern),
                    criteria.like(criteria.lower(root.get("correlationId")), pattern),
                    criteria.like(criteria.lower(root.get("templateKey")), pattern),
                    criteria.like(criteria.lower(root.get("sourceService")), pattern)));
        }
        if (sourceService != null && !sourceService.isBlank()) {
            specification = specification.and((root, ignored, criteria) -> criteria.equal(
                    criteria.lower(root.get("sourceService")),
                    sourceService.trim().toLowerCase(Locale.ROOT)));
        }
        if (templateKey != null && !templateKey.isBlank()) {
            specification = specification.and((root, ignored, criteria) -> criteria.equal(
                    criteria.upper(root.get("templateKey")),
                    templateKey.trim().toUpperCase(Locale.ROOT)));
        }
        if (status != null) {
            specification = specification.and((root, ignored, criteria) ->
                    criteria.equal(root.get("status"), status));
        }
        if (test != null) {
            specification = specification.and((root, ignored, criteria) ->
                    criteria.equal(root.get("test"), test));
        }
        if (from != null) {
            specification = specification.and((root, ignored, criteria) ->
                    criteria.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (to != null) {
            specification = specification.and((root, ignored, criteria) ->
                    criteria.lessThanOrEqualTo(root.get("createdAt"), to));
        }
        Page<NotificationRequest> requests = requestRepository.findAll(
                specification, PageRequest.of(
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
            long accepted,
            long confirmed,
            long delivered,
            long failed,
            long pending,
            long deadLetters,
            double deliveryRate,
            Map<DeliveryStatus, Long> deliveryStatuses,
            TemplateRegistry.RegistryHealth templateRegistry,
            TemplateContractCoverageService.CoverageReport coverage,
            int windowHours,
            boolean includeTest,
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
