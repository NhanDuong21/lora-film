package com.project.promotionservice.promotion.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.automation.dto.AutomationDtos.AnomalyCaseView;
import com.project.promotionservice.automation.entity.PromotionAutomationRun;
import com.project.promotionservice.automation.entity.PromotionPlaybook;
import com.project.promotionservice.automation.repository.PromotionAutomationRunRepository;
import com.project.promotionservice.automation.repository.PromotionPlaybookRepository;
import com.project.promotionservice.automation.service.PromotionAnomalyCaseService;
import com.project.promotionservice.common.audit.AuditTrailService;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.common.exception.ErrorCode;
import com.project.promotionservice.configuration.security.principal.UserPrincipal;
import com.project.promotionservice.promotion.dto.response.PromotionIssueResponse;
import com.project.promotionservice.promotion.entity.Promotion;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.enums.CampaignApprovalStatus;
import com.project.promotionservice.promotion.enums.CampaignScopeType;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.promotion.enums.PromotionDistributionMode;
import com.project.promotionservice.promotion.enums.PromotionStatus;
import com.project.promotionservice.promotion.manager.ManagerPromotionDtos.Automation;
import com.project.promotionservice.promotion.manager.ManagerPromotionDtos.Benefit;
import com.project.promotionservice.promotion.manager.ManagerPromotionDtos.Campaign;
import com.project.promotionservice.promotion.manager.ManagerPromotionDtos.Capabilities;
import com.project.promotionservice.promotion.manager.ManagerPromotionDtos.Incident;
import com.project.promotionservice.promotion.manager.ManagerPromotionDtos.Task;
import com.project.promotionservice.promotion.manager.ManagerPromotionDtos.Workspace;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import com.project.promotionservice.promotion.repository.PromotionRepository;
import com.project.promotionservice.promotion.service.PromotionCatalogService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds the manager promotion workspace from server-owned assignment claims.
 * A client-provided cinema id is only a requested view; it never grants scope.
 */
@Service
public class ManagerPromotionWorkspaceService {
    private static final Set<CampaignStatus> MANAGER_VISIBLE_STATUSES = Set.of(
            CampaignStatus.DRAFT, CampaignStatus.SCHEDULED, CampaignStatus.ACTIVE,
            CampaignStatus.PAUSED, CampaignStatus.COMPLETED);
    private static final Set<PromotionDistributionMode> MANUAL_DISTRIBUTION_MODES = Set.of(
            PromotionDistributionMode.ASSIGNED_WALLET,
            PromotionDistributionMode.PERSONAL_CODE);

    private final PromotionCampaignRepository campaignRepository;
    private final PromotionRepository promotionRepository;
    private final PromotionPlaybookRepository playbookRepository;
    private final PromotionAutomationRunRepository runRepository;
    private final PromotionAnomalyCaseService anomalyCaseService;
    private final PromotionCatalogService catalogService;
    private final AuditTrailService auditTrailService;
    private final ObjectMapper objectMapper;

    public ManagerPromotionWorkspaceService(
            PromotionCampaignRepository campaignRepository,
            PromotionRepository promotionRepository,
            PromotionPlaybookRepository playbookRepository,
            PromotionAutomationRunRepository runRepository,
            PromotionAnomalyCaseService anomalyCaseService,
            PromotionCatalogService catalogService,
            AuditTrailService auditTrailService,
            ObjectMapper objectMapper) {
        this.campaignRepository = campaignRepository;
        this.promotionRepository = promotionRepository;
        this.playbookRepository = playbookRepository;
        this.runRepository = runRepository;
        this.anomalyCaseService = anomalyCaseService;
        this.catalogService = catalogService;
        this.auditTrailService = auditTrailService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Workspace workspace(String requestedCinemaId, UserPrincipal principal) {
        String cinemaId = requireAssignedCinema(requestedCinemaId, principal);
        Capabilities capabilities = capabilities(principal);
        List<Campaign> campaigns = campaignsInternal(cinemaId, principal);
        List<Benefit> benefits = distributionOptionsInternal(cinemaId, principal);
        List<Incident> incidents = capabilities.canViewLocalIncidents()
                ? incidentsInternal(cinemaId) : List.of();
        List<Task> tasks = buildTasks(campaigns, benefits, incidents);
        long active = campaigns.stream().filter(item -> item.status() == CampaignStatus.ACTIVE).count();
        long upcoming = campaigns.stream().filter(item -> item.status() == CampaignStatus.SCHEDULED).count();
        long lowQuota = benefits.stream().filter(this::isLowQuota).count();
        return new Workspace(cinemaId, capabilities, active, upcoming, lowQuota,
                incidents.size(), tasks);
    }

    @Transactional(readOnly = true)
    public List<Campaign> campaigns(String requestedCinemaId, UserPrincipal principal) {
        return campaignsInternal(requireAssignedCinema(requestedCinemaId, principal), principal);
    }

    @Transactional(readOnly = true)
    public List<Automation> automations(String requestedCinemaId, UserPrincipal principal) {
        String cinemaId = requireAssignedCinema(requestedCinemaId, principal);
        Map<String, PromotionCampaign> campaigns = applicableCampaignEntities(cinemaId);
        return playbookRepository.findAllByDeletedAtIsNullOrderByCodeAsc().stream()
                .filter(item -> !Boolean.TRUE.equals(item.getTestData()))
                .filter(item -> playbookApplies(item, cinemaId, campaigns))
                .map(this::automationView)
                .sorted(Comparator.comparing(Automation::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Benefit> distributionOptions(
            String requestedCinemaId, UserPrincipal principal) {
        return distributionOptionsInternal(
                requireAssignedCinema(requestedCinemaId, principal), principal);
    }

    @Transactional(readOnly = true)
    public List<Incident> incidents(String requestedCinemaId, UserPrincipal principal) {
        return incidentsInternal(requireAssignedCinema(requestedCinemaId, principal));
    }

    @Transactional
    public PromotionIssueResponse issue(
            String requestedCinemaId,
            String promotionPublicId,
            List<String> userPublicIds,
            UserPrincipal principal) {
        String cinemaId = requireAssignedCinema(requestedCinemaId, principal);
        if (!capabilities(principal).canDistributeLocalBenefit()) {
            throw forbidden("Manager is not allowed to distribute local benefits");
        }
        Promotion promotion = promotionRepository
                .findByPublicIdAndDeletedAtIsNull(promotionPublicId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND, "Promotion not found", HttpStatus.NOT_FOUND));
        PromotionCampaign campaign = campaignRepository
                .findByPublicIdAndDeletedAtIsNull(promotion.getCampaignPublicId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND, "Campaign not found", HttpStatus.NOT_FOUND));
        if (!campaignApplies(campaign, cinemaId)
                || !promotionApplies(promotion, cinemaId)
                || !MANUAL_DISTRIBUTION_MODES.contains(promotion.getDistributionMode())) {
            throw forbidden("Benefit is outside the manager's local distribution scope");
        }
        if (!hasQuota(campaign) || !hasQuota(promotion)) {
            throw new BusinessException("PROMOTION_LOCAL_QUOTA_EXHAUSTED",
                    "The local benefit quota has been exhausted", HttpStatus.CONFLICT);
        }
        String actor = actor(principal);
        PromotionIssueResponse response = catalogService.issue(
                promotionPublicId, userPublicIds, actor);
        auditTrailService.record("MANAGER_PROMOTION_DISTRIBUTION", promotionPublicId,
                "LOCAL_BENEFIT_ISSUED", null,
                Map.of("managerAccountId", actor, "cinemaPublicId", cinemaId,
                        "issuedCount", response.issuedCount(),
                        "alreadyOwnedCount", response.alreadyOwnedCount()), actor);
        return response;
    }

    public String requireAssignedCinema(String requestedCinemaId, UserPrincipal principal) {
        if (principal == null || principal.getCinemaPublicIds().isEmpty()) {
            throw forbidden("Manager has no assigned cinema scope");
        }
        String requested = StringUtils.hasText(requestedCinemaId)
                ? requestedCinemaId.trim() : null;
        if (requested == null && principal.getCinemaPublicIds().size() == 1) {
            return principal.getCinemaPublicIds().iterator().next();
        }
        if (requested == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER,
                    "cinemaPublicId is required when a manager has multiple cinemas",
                    HttpStatus.BAD_REQUEST);
        }
        return principal.getCinemaPublicIds().stream()
                .filter(item -> item.equalsIgnoreCase(requested))
                .findFirst()
                .orElseThrow(() -> forbidden(
                        "Requested cinema is outside the manager's assigned scope"));
    }

    private List<Campaign> campaignsInternal(String cinemaId, UserPrincipal principal) {
        Map<String, PromotionCampaign> entities = applicableCampaignEntities(cinemaId);
        Map<String, List<Promotion>> benefitsByCampaign = new LinkedHashMap<>();
        promotionRepository.findAll().stream()
                .filter(item -> item.getDeletedAt() == null)
                .filter(item -> entities.containsKey(item.getCampaignPublicId()))
                .filter(item -> promotionApplies(item, cinemaId))
                .forEach(item -> benefitsByCampaign
                        .computeIfAbsent(item.getCampaignPublicId(), ignored -> new ArrayList<>())
                        .add(item));
        return entities.values().stream()
                .map(item -> campaignView(item,
                        benefitsByCampaign.getOrDefault(item.getPublicId(), List.of()),
                        principal))
                .sorted(Comparator.comparing(Campaign::startAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private Map<String, PromotionCampaign> applicableCampaignEntities(String cinemaId) {
        Map<String, PromotionCampaign> values = new LinkedHashMap<>();
        campaignRepository.findAll().stream()
                .filter(item -> item.getDeletedAt() == null)
                .filter(item -> !Boolean.TRUE.equals(item.getTestData()))
                .filter(item -> MANAGER_VISIBLE_STATUSES.contains(item.getStatus()))
                .filter(item -> campaignApplies(item, cinemaId))
                .forEach(item -> values.put(item.getPublicId(), item));
        return values;
    }

    private List<Benefit> distributionOptionsInternal(
            String cinemaId, UserPrincipal principal) {
        Map<String, PromotionCampaign> campaigns = applicableCampaignEntities(cinemaId);
        boolean canDistribute = capabilities(principal).canDistributeLocalBenefit();
        return promotionRepository.findAll().stream()
                .filter(item -> item.getDeletedAt() == null)
                .filter(item -> campaigns.containsKey(item.getCampaignPublicId()))
                .filter(item -> MANUAL_DISTRIBUTION_MODES.contains(item.getDistributionMode()))
                .filter(item -> promotionApplies(item, cinemaId))
                .map(item -> benefitView(item, campaigns.get(item.getCampaignPublicId()),
                        canDistribute && item.getStatus() == PromotionStatus.ACTIVE
                                && campaigns.get(item.getCampaignPublicId()).getStatus()
                                == CampaignStatus.ACTIVE
                                && hasQuota(item)
                                && hasQuota(campaigns.get(item.getCampaignPublicId()))))
                .sorted(Comparator.comparing(Benefit::validTo,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private List<Incident> incidentsInternal(String cinemaId) {
        Map<String, PromotionCampaign> campaigns = applicableCampaignEntities(cinemaId);
        Map<String, PromotionAutomationRun> runs = new LinkedHashMap<>();
        runRepository.findTop20ByOrderByCreatedAtDesc().stream()
                .filter(item -> !Boolean.TRUE.equals(item.getTestData()))
                .filter(item -> campaigns.containsKey(item.getCampaignPublicId()))
                .filter(item -> jsonScopeApplies(item.getScopeSnapshotJson(), cinemaId))
                .forEach(item -> runs.put(item.getPublicId(), item));
        return anomalyCaseService.openCases(false).stream()
                .filter(item -> runs.containsKey(item.runPublicId()))
                .map(this::incidentView)
                .toList();
    }

    private Campaign campaignView(
            PromotionCampaign item, List<Promotion> promotions,
            UserPrincipal principal) {
        List<Benefit> benefits = promotions.stream()
                .map(promotion -> benefitView(promotion, item, false))
                .toList();
        return new Campaign(item.getPublicId(), item.getCode(), item.getName(),
                item.getDescription(), item.getStatus(), item.getApprovalStatus(),
                item.getScopeType() == CampaignScopeType.GLOBAL ? "CENTRAL" : "LOCAL",
                true, item.getStartAt(), item.getEndAt(), item.getMaxRedemptions(),
                item.getRedemptionCount(), remaining(item.getMaxRedemptions(),
                item.getRedemptionCount()), benefits);
    }

    private Benefit benefitView(
            Promotion item, PromotionCampaign campaign, boolean canDistribute) {
        return new Benefit(item.getPublicId(), item.getCampaignPublicId(),
                campaign.getName(), item.getName(), item.getDescription(),
                item.getPromotionType(), item.getStatus(), item.getDistributionMode(),
                item.getValidFrom(), item.getValidTo(), item.getMaxRedemptions(),
                item.getRedemptionCount(), remaining(item.getMaxRedemptions(),
                item.getRedemptionCount()), canDistribute,
                guidance(item.getDistributionMode()));
    }

    private Automation automationView(PromotionPlaybook item) {
        PromotionAutomationRun latest = runRepository
                .findByPlaybookPublicIdOrderByCreatedAtDesc(item.getPublicId()).stream()
                .filter(run -> !Boolean.TRUE.equals(run.getTestData()))
                .findFirst().orElse(null);
        return new Automation(item.getPublicId(), item.getCode(), item.getName(),
                item.getDescription(), item.getStatus(), item.getTriggerType(),
                latest == null ? null : latest.getStatus(),
                latest == null ? null : (latest.getCompletedAt() == null
                        ? latest.getStartedAt() : latest.getCompletedAt()), true);
    }

    private Incident incidentView(AnomalyCaseView item) {
        return new Incident(item.publicId(), item.businessName(), item.summary(),
                item.status(), item.assignedTo(), item.createdAt());
    }

    private List<Task> buildTasks(
            List<Campaign> campaigns, List<Benefit> benefits,
            List<Incident> incidents) {
        List<Task> tasks = new ArrayList<>();
        Instant soon = Instant.now().plus(7, ChronoUnit.DAYS);
        campaigns.stream()
                .filter(item -> item.status() == CampaignStatus.SCHEDULED)
                .filter(item -> item.startAt() != null && item.startAt().isBefore(soon))
                .forEach(item -> tasks.add(new Task("campaign:" + item.publicId(),
                        "UPCOMING_CAMPAIGN", item.name(),
                        "Chương trình sắp áp dụng tại rạp; hãy phổ biến cho nhân viên.",
                        "campaigns", item.startAt())));
        campaigns.stream()
                .filter(item -> item.approvalStatus() == CampaignApprovalStatus.REJECTED)
                .forEach(item -> tasks.add(new Task("proposal:" + item.publicId(),
                        "RETURNED_PROPOSAL", item.name(),
                        "Đề xuất địa phương đã được trả lại và cần cập nhật.",
                        "campaigns", null)));
        benefits.stream().filter(this::isLowQuota).forEach(item -> tasks.add(
                new Task("quota:" + item.publicId(), "LOW_QUOTA", item.name(),
                        "Hạn mức ưu đãi tại rạp sắp hết.", "distribution", item.validTo())));
        incidents.forEach(item -> tasks.add(new Task("incident:" + item.publicId(),
                "LOCAL_INCIDENT", item.businessName(), item.summary(),
                "incidents", item.createdAt())));
        return tasks.stream()
                .sorted(Comparator.comparing(Task::dueAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(20)
                .toList();
    }

    private Capabilities capabilities(UserPrincipal principal) {
        Set<String> values = principal == null
                ? Set.of() : new LinkedHashSet<>(principal.getPermissions());
        return new Capabilities(values.contains("PROMOTION_VIEW"),
                values.contains("PROMOTION_LAUNCH_APPROVED_TEMPLATE"),
                values.contains("PROMOTION_DISTRIBUTE_LOCAL"),
                values.contains("PROMOTION_AUDIT_VIEW"),
                values.contains("PROMOTION_AUTHOR"));
    }

    private boolean campaignApplies(PromotionCampaign campaign, String cinemaId) {
        if (campaign.getScopeType() == CampaignScopeType.GLOBAL) return true;
        return jsonValues(campaign.getCinemaScopeJson(), "cinemaPublicIds", "cinemaIds")
                .stream().anyMatch(cinemaId::equalsIgnoreCase);
    }

    private boolean promotionApplies(Promotion promotion, String cinemaId) {
        if (!validScopeJson(promotion.getConditionsJson())) return false;
        Set<String> targets = jsonValues(
                promotion.getConditionsJson(), "cinemaPublicIds", "cinemaIds", "cinemaId");
        return targets.isEmpty() || targets.stream().anyMatch(cinemaId::equalsIgnoreCase);
    }

    private boolean playbookApplies(
            PromotionPlaybook playbook, String cinemaId,
            Map<String, PromotionCampaign> campaigns) {
        if (StringUtils.hasText(playbook.getCampaignPublicId())) {
            return campaigns.containsKey(playbook.getCampaignPublicId())
                    && jsonScopeApplies(playbook.getScopeJson(), cinemaId);
        }
        return jsonScopeApplies(playbook.getScopeJson(), cinemaId);
    }

    private boolean jsonScopeApplies(String json, String cinemaId) {
        if (!validScopeJson(json)) return false;
        Set<String> targets = jsonValues(json,
                "cinemaPublicIds", "cinemaIds", "cinemaId");
        return targets.isEmpty() || targets.stream().anyMatch(cinemaId::equalsIgnoreCase);
    }

    private boolean validScopeJson(String json) {
        if (!StringUtils.hasText(json)) return false;
        try {
            JsonNode node = objectMapper.readTree(json);
            while (node != null && node.isTextual()) node = objectMapper.readTree(node.asText());
            return node != null && (node.isObject() || node.isArray());
        } catch (Exception ignored) {
            return false;
        }
    }

    private Set<String> jsonValues(String json, String... keys) {
        if (!StringUtils.hasText(json)) return Set.of();
        try {
            JsonNode node = objectMapper.readTree(json);
            while (node != null && node.isTextual()) node = objectMapper.readTree(node.asText());
            if (node == null) return Set.of();
            JsonNode target = node;
            if (node.isObject()) {
                target = null;
                for (String key : keys) {
                    if (node.has(key)) {
                        target = node.get(key);
                        break;
                    }
                }
            }
            if (target == null) return Set.of();
            Set<String> values = new LinkedHashSet<>();
            if (target.isArray()) {
                target.forEach(value -> addJsonValue(values, value));
            } else {
                addJsonValue(values, target);
            }
            return Set.copyOf(values);
        } catch (Exception ignored) {
            return Set.of();
        }
    }

    private void addJsonValue(Set<String> values, JsonNode value) {
        if (value != null && value.isTextual() && StringUtils.hasText(value.asText())) {
            values.add(value.asText().trim());
        }
    }

    private boolean hasQuota(PromotionCampaign item) {
        return item.getMaxRedemptions() == null
                || item.getRedemptionCount() < item.getMaxRedemptions();
    }

    private boolean hasQuota(Promotion item) {
        return item.getMaxRedemptions() == null
                || item.getRedemptionCount() < item.getMaxRedemptions();
    }

    private Integer remaining(Integer quota, Integer used) {
        if (quota == null) return null;
        return Math.max(0, quota - (used == null ? 0 : used));
    }

    private boolean isLowQuota(Benefit item) {
        return item.quota() != null && item.quota() > 0 && item.remaining() != null
                && item.remaining() <= Math.max(1, (int) Math.ceil(item.quota() * 0.2));
    }

    private String guidance(PromotionDistributionMode mode) {
        return mode == PromotionDistributionMode.PERSONAL_CODE
                ? "Gửi mã riêng cho khách đã được chọn; không chia sẻ công khai."
                : "Chỉ cấp vào ví khách khi xử lý tại rạp và trong hạn mức còn lại.";
    }

    private String actor(UserPrincipal principal) {
        if (principal != null && principal.getId() != null) return principal.getId().toString();
        if (principal != null && principal.getUsername() != null) {
            String username = principal.getUsername();
            return username.length() <= 36 ? username : username.substring(0, 36);
        }
        return "SYSTEM";
    }

    private BusinessException forbidden(String message) {
        return new BusinessException(ErrorCode.FORBIDDEN, message, HttpStatus.FORBIDDEN);
    }
}
