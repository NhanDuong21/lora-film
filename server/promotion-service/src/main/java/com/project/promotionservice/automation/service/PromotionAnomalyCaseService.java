package com.project.promotionservice.automation.service;

import com.project.promotionservice.automation.dto.AutomationDtos.AnomalyCaseView;
import com.project.promotionservice.automation.dto.AutomationDtos.ResolveAnomalyRequest;
import com.project.promotionservice.automation.entity.PromotionAnomalyCase;
import com.project.promotionservice.automation.entity.PromotionAudienceMember;
import com.project.promotionservice.automation.entity.PromotionAutomationRun;
import com.project.promotionservice.automation.enums.AnomalyCaseStatus;
import com.project.promotionservice.automation.enums.AnomalyResolution;
import com.project.promotionservice.automation.repository.PromotionAnomalyCaseRepository;
import com.project.promotionservice.automation.client.AutomationActorDirectoryClient;
import com.project.promotionservice.common.audit.AuditTrailService;
import com.project.promotionservice.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class PromotionAnomalyCaseService {
    private static final List<AnomalyCaseStatus> OPEN_STATUSES =
            List.of(AnomalyCaseStatus.OPEN, AnomalyCaseStatus.IN_REVIEW);

    private final PromotionAnomalyCaseRepository repository;
    private final AutomationActorDirectoryClient actorDirectory;
    private final AuditTrailService auditTrailService;

    public PromotionAnomalyCaseService(
            PromotionAnomalyCaseRepository repository,
            AutomationActorDirectoryClient actorDirectory,
            AuditTrailService auditTrailService) {
        this.repository = repository;
        this.actorDirectory = actorDirectory;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public PromotionAnomalyCase open(
            PromotionAutomationRun run, PromotionAudienceMember member) {
        PromotionAnomalyCase existing = repository
                .findByAudienceMemberPublicId(member.getPublicId()).orElse(null);
        if (existing != null) return existing;
        PromotionAnomalyCase item = new PromotionAnomalyCase();
        item.setRunPublicId(run.getPublicId());
        item.setAudienceMemberPublicId(member.getPublicId());
        item.setPlaybookCode(run.getPlaybookCode());
        item.setCustomerPublicId(member.getCustomerPublicId());
        item.setSourceReference(run.getTriggerReference());
        item.setReasonCode(member.getReasonCode());
        item.setCostAmount(member.getBudgetReservedAmount() == null
                ? BigDecimal.ZERO : member.getBudgetReservedAmount());
        item.setTestData(Boolean.TRUE.equals(run.getTestData()));
        item.setEnvironmentTag(normalizeEnvironment(run.getEnvironmentTag(), run.getTestData()));
        item.setCreatedBy("SYSTEM");
        item.setUpdatedBy("SYSTEM");
        PromotionAnomalyCase saved = repository.save(item);
        auditTrailService.record("PROMOTION_ANOMALY_CASE", saved.getPublicId(),
                "ANOMALY_CASE_OPEN", null, audit(saved), "SYSTEM");
        return saved;
    }

    @Transactional(readOnly = true)
    public List<AnomalyCaseView> openCases(boolean includeTestData) {
        List<PromotionAnomalyCase> cases = includeTestData
                ? repository.findByStatusInOrderByCreatedAtDesc(OPEN_STATUSES)
                : repository.findByStatusInAndTestDataOrderByCreatedAtDesc(
                        OPEN_STATUSES, false);
        return cases.stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public long countOpen(String runPublicId) {
        return repository.countByRunPublicIdAndStatusIn(runPublicId, OPEN_STATUSES);
    }

    @Transactional
    public AnomalyCaseView assign(String publicId, String actor) {
        PromotionAnomalyCase item = require(publicId);
        requireOpen(item);
        if (item.getAssignedTo() != null && !item.getAssignedTo().equals(actor)) {
            throw conflict("Vụ việc đang được một nhân viên khác xử lý");
        }
        Map<String, Object> before = audit(item);
        item.setAssignedTo(actor);
        item.setStatus(AnomalyCaseStatus.IN_REVIEW);
        item.setUpdatedBy(actor);
        PromotionAnomalyCase saved = repository.save(item);
        auditTrailService.record("PROMOTION_ANOMALY_CASE", saved.getPublicId(),
                "ANOMALY_CASE_ASSIGN", before, audit(saved), actor);
        return view(saved);
    }

    @Transactional
    public AnomalyCaseView resolve(
            String publicId, ResolveAnomalyRequest request, String actor) {
        PromotionAnomalyCase item = require(publicId);
        requireOpen(item);
        if (request.resolution() == AnomalyResolution.TEST_DATA
                && !Boolean.TRUE.equals(item.getTestData())) {
            throw conflict("Chỉ vụ việc UAT mới có thể đóng vì dữ liệu kiểm thử");
        }
        Map<String, Object> before = audit(item);
        item.setAssignedTo(item.getAssignedTo() == null ? actor : item.getAssignedTo());
        item.setResolution(request.resolution());
        item.setResolutionNote(request.resolutionNote().trim());
        item.setResolvedBy(actor);
        item.setResolvedAt(Instant.now());
        item.setStatus(statusFor(request.resolution()));
        item.setUpdatedBy(actor);
        PromotionAnomalyCase saved = repository.save(item);
        auditTrailService.record("PROMOTION_ANOMALY_CASE", saved.getPublicId(),
                "ANOMALY_CASE_RESOLVE", before, audit(saved), actor);
        return view(saved);
    }

    private AnomalyCaseStatus statusFor(AnomalyResolution resolution) {
        return switch (resolution) {
            case ACCEPTED_COST -> AnomalyCaseStatus.RESOLVED_ACCEPTED_COST;
            case CUSTOMER_ABUSE -> AnomalyCaseStatus.RESOLVED_CUSTOMER_ABUSE;
            case TEST_DATA -> AnomalyCaseStatus.DISMISSED_TEST_DATA;
        };
    }

    private PromotionAnomalyCase require(String publicId) {
        return repository.findByPublicId(publicId).orElseThrow(() ->
                new BusinessException("ANOMALY_CASE_NOT_FOUND",
                        "Không tìm thấy vụ việc cần đối soát", HttpStatus.NOT_FOUND));
    }

    private void requireOpen(PromotionAnomalyCase item) {
        if (!item.getStatus().isOpen()) {
            throw conflict("Vụ việc đã được đóng và không thể xử lý lại");
        }
    }

    private BusinessException conflict(String message) {
        return new BusinessException("ANOMALY_CASE_CONFLICT", message,
                HttpStatus.CONFLICT);
    }

    private AnomalyCaseView view(PromotionAnomalyCase item) {
        return new AnomalyCaseView(item.getPublicId(), item.getRunPublicId(),
                item.getAudienceMemberPublicId(), item.getPlaybookCode(),
                businessName(item.getPlaybookCode()), item.getCustomerPublicId(),
                item.getSourceReference(), summary(item.getReasonCode()),
                item.getReasonCode(), item.getCostAmount(), item.getTestData(),
                item.getEnvironmentTag(), item.getStatus(), item.getAssignedTo(),
                actorDirectory.displayName(item.getAssignedTo()), item.getResolution(),
                item.getResolutionNote(), item.getResolvedBy(),
                actorDirectory.displayName(item.getResolvedBy()), item.getResolvedAt(),
                item.getCreatedAt());
    }

    private String businessName(String playbookCode) {
        return PromotionAutomationService.SECOND_BOOKING.equals(playbookCode)
                ? "Ưu đãi cho lần đặt vé thứ hai" : "Quà sinh nhật";
    }

    private String summary(String reasonCode) {
        if ("SOURCE_BOOKING_REFUNDED_AFTER_BENEFIT_USED".equals(reasonCode)) {
            return "Booking đầu tiên đã được hoàn tiền sau khi ưu đãi cho lần đặt vé thứ hai được sử dụng.";
        }
        return "Quyền lợi đã phát sinh chi phí và cần nhân viên kiểm tra.";
    }

    private String normalizeEnvironment(String value, Boolean testData) {
        if (value != null && !value.isBlank()) return value;
        return Boolean.TRUE.equals(testData) ? "UAT" : "BUSINESS";
    }

    private Map<String, Object> audit(PromotionAnomalyCase item) {
        return Map.ofEntries(
                Map.entry("status", item.getStatus()),
                Map.entry("testData", Boolean.TRUE.equals(item.getTestData())),
                Map.entry("assignedTo", value(item.getAssignedTo())),
                Map.entry("resolution", value(item.getResolution())),
                Map.entry("resolutionNote", value(item.getResolutionNote())),
                Map.entry("resolvedBy", value(item.getResolvedBy())),
                Map.entry("resolvedAt", value(item.getResolvedAt())));
    }

    private Object value(Object value) {
        return value == null ? "" : value;
    }
}
