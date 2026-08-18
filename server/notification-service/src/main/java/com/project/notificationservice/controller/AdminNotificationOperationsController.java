package com.project.notificationservice.controller;

import com.project.notificationservice.api.ApiResponse;
import com.project.notificationservice.domain.NotificationTypes.Channel;
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
import java.util.LinkedHashMap;
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
        Map<Channel, long[]> channelCounters = new EnumMap<>(Channel.class);
        deliveryRepository.countOperationalByChannelAndStatus(since, includeTest).forEach(row -> {
            Channel channel = (Channel) row[0];
            DeliveryStatus status = (DeliveryStatus) row[1];
            long count = ((Number) row[2]).longValue();
            long[] values = channelCounters.computeIfAbsent(channel, ignored -> new long[5]);
            values[0] += count;
            if (status == DeliveryStatus.SENT || status == DeliveryStatus.DELIVERED) values[1] += count;
            if (status == DeliveryStatus.DELIVERED) values[2] += count;
            if (status == DeliveryStatus.FAILED || status == DeliveryStatus.DEAD_LETTERED) values[3] += count;
            if (status == DeliveryStatus.PENDING || status == DeliveryStatus.RETRY_SCHEDULED) values[4] += count;
        });
        Map<Channel, ChannelSummary> channels = new LinkedHashMap<>();
        channelCounters.forEach((channel, values) -> channels.put(channel,
                new ChannelSummary(values[0], values[1], values[2], values[3], values[4])));
        TemplateContractCoverageService.CoverageReport coverage = coverageService.inspect();
        long openDeadLetters = deadLetterRepository.countByReprocessedAtIsNull();
        long activeIncidents = openDeadLetters + coverage.blockedRequirements();
        return ApiResponse.success(new Dashboard(
                requests,
                total,
                accepted,
                confirmed,
                accepted,
                failures,
                counts.get(DeliveryStatus.PENDING) + counts.get(DeliveryStatus.RETRY_SCHEDULED),
                counts.get(DeliveryStatus.DEAD_LETTERED),
                activeIncidents,
                Math.round(rate * 100.0) / 100.0,
                Map.copyOf(counts),
                Map.copyOf(channels),
                templateRegistry.health(),
                coverage,
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
                    criteria.like(criteria.lower(root.get("sourceService")), pattern),
                    criteria.like(criteria.lower(root.get("payloadJson")), pattern)));
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
                request.isTest(), service.recipientSummary(request.getId()),
                request.getExpiresAt(), request.getCreatedAt())));
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
    public ApiResponse<Page<NotificationApplicationService.AttentionDetails>> deadLetters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return ApiResponse.success(deadLetterRepository.findAllByReprocessedAtIsNull(PageRequest.of(
                Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"))).map(service::attentionDetails));
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
            long activeIncidents,
            double deliveryRate,
            Map<DeliveryStatus, Long> deliveryStatuses,
            Map<Channel, ChannelSummary> channels,
            TemplateRegistry.RegistryHealth templateRegistry,
            TemplateContractCoverageService.CoverageReport coverage,
            int windowHours,
            boolean includeTest,
            Instant generatedAt) {
    }

    public record ChannelSummary(
            long total,
            long accepted,
            long confirmed,
            long failed,
            long pending) {
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
            NotificationApplicationService.RecipientSummary recipient,
            Instant expiresAt,
            Instant createdAt) {
    }
}
