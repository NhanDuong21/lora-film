package com.project.promotionservice.automation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.project.promotionservice.automation.client.BirthdayAudienceClient;
import com.project.promotionservice.automation.client.AutomationActorDirectoryClient;
import com.project.promotionservice.automation.dto.AutomationDtos.*;
import com.project.promotionservice.automation.entity.*;
import com.project.promotionservice.automation.enums.*;
import com.project.promotionservice.automation.repository.*;
import com.project.promotionservice.common.audit.AuditTrailService;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.promotion.entity.Promotion;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.enums.CampaignApprovalStatus;
import com.project.promotionservice.promotion.enums.PromotionType;
import com.project.promotionservice.promotion.enums.PromotionRedemptionStatus;
import com.project.promotionservice.promotion.enums.UserPromotionStatus;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import com.project.promotionservice.promotion.repository.PromotionRepository;
import com.project.promotionservice.promotion.repository.PromotionRedemptionRepository;
import com.project.promotionservice.promotion.repository.UserPromotionRepository;
import com.project.promotionservice.promotion.service.PromotionCatalogService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.math.BigDecimal;
import java.security.MessageDigest;

@Service
public class PromotionAutomationService {
    public static final String BIRTHDAY = "BIRTHDAY_REWARD";
    public static final String SECOND_BOOKING = "SECOND_BOOKING_INCENTIVE";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final PromotionPlaybookRepository playbookRepository;
    private final PromotionAutomationRunRepository runRepository;
    private final PromotionAudienceSnapshotRepository snapshotRepository;
    private final PromotionAudienceMemberRepository memberRepository;
    private final PromotionIssueJobRepository jobRepository;
    private final PromotionAutomationSuppressionRepository suppressionRepository;
    private final PromotionCampaignRepository campaignRepository;
    private final PromotionRepository promotionRepository;
    private final UserPromotionRepository walletRepository;
    private final PromotionRedemptionRepository redemptionRepository;
    private final BirthdayAudienceClient birthdayClient;
    private final AutomationActorDirectoryClient actorDirectory;
    private final ObjectMapper objectMapper;
    private final PromotionCatalogService catalogService;
    private final PromotionAutomationBudgetService budgetService;
    private final AuditTrailService auditTrailService;

    public PromotionAutomationService(
            PromotionPlaybookRepository playbookRepository,
            PromotionAutomationRunRepository runRepository,
            PromotionAudienceSnapshotRepository snapshotRepository,
            PromotionAudienceMemberRepository memberRepository,
            PromotionIssueJobRepository jobRepository,
            PromotionAutomationSuppressionRepository suppressionRepository,
            PromotionCampaignRepository campaignRepository,
            PromotionRepository promotionRepository,
            UserPromotionRepository walletRepository,
            PromotionRedemptionRepository redemptionRepository,
            BirthdayAudienceClient birthdayClient,
            AutomationActorDirectoryClient actorDirectory,
            ObjectMapper objectMapper,
            PromotionCatalogService catalogService,
            PromotionAutomationBudgetService budgetService,
            AuditTrailService auditTrailService) {
        this.playbookRepository = playbookRepository;
        this.runRepository = runRepository;
        this.snapshotRepository = snapshotRepository;
        this.memberRepository = memberRepository;
        this.jobRepository = jobRepository;
        this.suppressionRepository = suppressionRepository;
        this.campaignRepository = campaignRepository;
        this.promotionRepository = promotionRepository;
        this.walletRepository = walletRepository;
        this.redemptionRepository = redemptionRepository;
        this.birthdayClient = birthdayClient;
        this.actorDirectory = actorDirectory;
        this.objectMapper = objectMapper;
        this.catalogService = catalogService;
        this.budgetService = budgetService;
        this.auditTrailService = auditTrailService;
    }

    @Transactional(readOnly = true)
    public List<PlaybookView> playbooks() {
        return playbookRepository.findAllByDeletedAtIsNullOrderByCodeAsc()
                .stream().map(this::view).toList();
    }

    @Transactional
    public PlaybookView save(String publicId, PlaybookRequest request, String actor) {
        validateJson(request.configJson(), "configJson");
        validateJson(request.scopeJson(), "scopeJson");
        PromotionPlaybook playbook;
        Map<String, Object> before = null;
        if (publicId == null) {
            if (playbookRepository.findByCodeAndDeletedAtIsNull(request.code()).isPresent()) {
                throw conflict("Playbook code already exists");
            }
            playbook = new PromotionPlaybook();
            playbook.setCode(request.code());
            playbook.setCreatedBy(actor);
        } else {
            playbook = requirePlaybook(publicId);
            before = playbookAudit(playbook);
            if (!playbook.getCode().equals(request.code())) {
                throw conflict("Playbook code is immutable");
            }
            playbook.setPlaybookVersion(playbook.getPlaybookVersion() + 1);
        }
        playbook.setName(request.name());
        playbook.setDescription(request.description());
        playbook.setTriggerType(request.triggerType().toUpperCase(Locale.ROOT));
        playbook.setCampaignPublicId(blankToNull(request.campaignPublicId()));
        playbook.setPromotionPublicId(blankToNull(request.promotionPublicId()));
        playbook.setConfigJson(request.configJson());
        playbook.setScopeJson(request.scopeJson());
        playbook.setBudgetLimit(request.budgetLimit());
        playbook.setQuotaLimit(request.quotaLimit());
        playbook.setConfigHash(configurationHash(playbook, null, null));
        playbook.setStatus(PlaybookStatus.DRAFT);
        playbook.setSubmittedBy(null);
        playbook.setSubmittedAt(null);
        playbook.setSubmittedPlaybookVersion(null);
        playbook.setSubmittedConfigHash(null);
        playbook.setApprovedBy(null);
        playbook.setApprovedAt(null);
        playbook.setApprovedPlaybookVersion(null);
        playbook.setApprovedConfigHash(null);
        playbook.setUpdatedBy(actor);
        PromotionPlaybook saved = playbookRepository.save(playbook);
        auditTrailService.record("PROMOTION_PLAYBOOK", saved.getPublicId(),
                publicId == null ? "PLAYBOOK_CREATE" : "PLAYBOOK_UPDATE",
                before, playbookAudit(saved), actor);
        return view(saved);
    }

    @Transactional
    public PlaybookView submit(String publicId, String actor) {
        PromotionPlaybook playbook = requirePlaybook(publicId);
        if (playbook.getStatus() != PlaybookStatus.DRAFT
                && playbook.getStatus() != PlaybookStatus.REJECTED) {
            throw conflict("Only a draft or rejected playbook can be submitted");
        }
        Map<String, Object> before = playbookAudit(playbook);
        ConfiguredReferences references = requireConfiguredReferences(playbook);
        String configHash = configurationHash(
                playbook, references.campaign(), references.promotion());
        estimatedUnitCost(references.promotion());
        playbook.setConfigHash(configHash);
        playbook.setStatus(PlaybookStatus.PENDING_APPROVAL);
        playbook.setSubmittedBy(actor);
        playbook.setSubmittedAt(Instant.now());
        playbook.setSubmittedPlaybookVersion(playbook.getPlaybookVersion());
        playbook.setSubmittedConfigHash(configHash);
        playbook.setUpdatedBy(actor);
        PromotionPlaybook saved = playbookRepository.save(playbook);
        auditTrailService.record("PROMOTION_PLAYBOOK", saved.getPublicId(),
                "PLAYBOOK_SUBMIT", before, playbookAudit(saved), actor);
        return view(saved);
    }

    @Transactional
    public PlaybookView approve(String publicId, String actor) {
        PromotionPlaybook playbook = requirePlaybook(publicId);
        if (playbook.getStatus() != PlaybookStatus.PENDING_APPROVAL) {
            throw conflict("Playbook is not pending approval");
        }
        if (actor.equalsIgnoreCase(playbook.getSubmittedBy())) {
            throw conflict("Người gửi duyệt không thể tự phê duyệt cùng phiên bản playbook");
        }
        Map<String, Object> before = playbookAudit(playbook);
        ConfiguredReferences references = requireConfiguredReferences(playbook);
        String currentHash = configurationHash(
                playbook, references.campaign(), references.promotion());
        if (!Objects.equals(playbook.getSubmittedPlaybookVersion(),
                playbook.getPlaybookVersion())
                || !Objects.equals(playbook.getSubmittedConfigHash(), currentHash)) {
            throw conflict("Cấu hình đã thay đổi sau khi gửi duyệt; hãy lưu và gửi lại phiên bản mới");
        }
        estimatedUnitCost(references.promotion());
        playbook.setConfigHash(currentHash);
        playbook.setStatus(PlaybookStatus.ACTIVE);
        playbook.setApprovedBy(actor);
        playbook.setApprovedAt(Instant.now());
        playbook.setApprovedPlaybookVersion(playbook.getPlaybookVersion());
        playbook.setApprovedConfigHash(currentHash);
        playbook.setUpdatedBy(actor);
        PromotionPlaybook saved = playbookRepository.save(playbook);
        auditTrailService.record("PROMOTION_PLAYBOOK", saved.getPublicId(),
                "PLAYBOOK_APPROVE", before, playbookAudit(saved), actor);
        return view(saved);
    }

    @Transactional
    public PlaybookView pause(String publicId, String actor) {
        PromotionPlaybook playbook = requirePlaybook(publicId);
        if (playbook.getStatus() != PlaybookStatus.ACTIVE) {
            throw conflict("Only an active playbook can be paused");
        }
        Map<String, Object> before = playbookAudit(playbook);
        playbook.setStatus(PlaybookStatus.PAUSED);
        playbook.setUpdatedBy(actor);
        PromotionPlaybook saved = playbookRepository.save(playbook);
        auditTrailService.record("PROMOTION_PLAYBOOK", saved.getPublicId(),
                "PLAYBOOK_PAUSE", before, playbookAudit(saved), actor);
        return view(saved);
    }

    @Transactional(readOnly = true)
    public List<RunView> recentRuns() {
        return runRepository.findTop20ByOrderByCreatedAtDesc()
                .stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public RunView run(String publicId) {
        return view(requireRun(publicId));
    }

    @Transactional(readOnly = true)
    public CampaignAutomationView campaignAutomation(String campaignPublicId) {
        List<PromotionPlaybook> playbooks = playbookRepository
                .findByCampaignPublicIdAndDeletedAtIsNullOrderByCodeAsc(campaignPublicId);
        List<PromotionAutomationRun> runs = runRepository
                .findByCampaignPublicIdOrderByCreatedAtDesc(campaignPublicId);
        List<PlaybookView> playbookViews = playbooks.stream().map(this::view).toList();
        PromotionCampaign campaign = campaignRepository
                .findByPublicIdAndDeletedAtIsNull(campaignPublicId).orElse(null);
        EntitlementSummary entitlements = aggregateEntitlements(
                playbooks, campaign == null ? null : campaign.getBudgetAmount());
        return new CampaignAutomationView(
                playbookViews, runs.isEmpty() ? null : view(runs.getFirst()), entitlements);
    }

    @Transactional
    public PromotionAutomationRun createBirthdayRun(LocalDate date) {
        PromotionPlaybook playbook = activeByCode(BIRTHDAY);
        Integer effectiveQuota = quotaView(playbook).effectiveQuota();
        int limit = effectiveQuota == null ? 100_000 : effectiveQuota;
        if (limit <= 0) {
            throw conflict("Không còn hạn mức hiệu lực để tạo lần chạy sinh nhật");
        }
        List<String> customers = birthdayClient.findEligible(date, limit);
        return createRun(playbook, "SCHEDULE:" + date, customers,
                BIRTHDAY + ":" + date,
                customer -> BIRTHDAY + ":" + customer + ":" + date.getYear());
    }

    @Transactional
    public PromotionAutomationRun createSecondBookingRun(
            String customerId, String bookingReference) {
        if (suppressionRepository.existsByPlaybookCodeAndTriggerReference(
                SECOND_BOOKING, bookingReference)) {
            return null;
        }
        PromotionPlaybook playbook = activeByCode(SECOND_BOOKING);
        String key = SECOND_BOOKING + ":" + customerId;
        return createRun(playbook, bookingReference, List.of(customerId), key,
                customer -> key);
    }

    @Transactional
    public void revokeSecondBookingForRefund(String bookingReference) {
        PromotionAutomationSuppression suppression = suppressionRepository
                .findByPlaybookCodeAndTriggerReference(SECOND_BOOKING, bookingReference)
                .orElseGet(PromotionAutomationSuppression::new);
        suppression.setPlaybookCode(SECOND_BOOKING);
        suppression.setTriggerReference(bookingReference);
        suppression.setReasonCode("SOURCE_BOOKING_REFUNDED");
        suppression.setObservedAt(Instant.now());
        suppression.setCreatedBy("SYSTEM");
        suppression.setUpdatedBy("SYSTEM");
        suppressionRepository.save(suppression);
        PromotionAutomationRun run = runRepository
                .findByPlaybookCodeAndTriggerReference(SECOND_BOOKING, bookingReference)
                .orElse(null);
        if (run == null) return;
        PromotionAudienceMember member = memberRepository
                .findFirstByRunPublicIdOrderByIdAsc(run.getPublicId()).orElse(null);
        if (member != null) {
            PromotionCatalogService.AutomationCompensationOutcome outcome =
                    catalogService.compensateAutomationIssuance(
                    member.getIssuanceKey(), "SOURCE_BOOKING_REFUNDED");
            if (outcome == PromotionCatalogService.AutomationCompensationOutcome.REVOKED) {
                budgetService.releaseForMember(
                        member.getPublicId(), run.getPlaybookPublicId());
                member.setStatus(AudienceMemberStatus.SKIPPED_INELIGIBLE);
                member.setReasonCode("SOURCE_BOOKING_REFUNDED_REVOKED");
            } else if (outcome
                    == PromotionCatalogService.AutomationCompensationOutcome.REVOCATION_PENDING) {
                member.setStatus(AudienceMemberStatus.REVOCATION_PENDING);
                member.setReasonCode("SOURCE_BOOKING_REFUNDED_WAITING_FOR_RESERVATION");
            } else if (outcome
                    == PromotionCatalogService.AutomationCompensationOutcome.ANOMALY_REVIEW_REQUIRED) {
                member.setStatus(AudienceMemberStatus.ANOMALY_REVIEW_REQUIRED);
                member.setReasonCode("SOURCE_BOOKING_REFUNDED_AFTER_BENEFIT_USED");
            } else {
                member.setReasonCode("SOURCE_BOOKING_REFUNDED_" + outcome.name());
            }
            member.setUpdatedBy("SYSTEM");
            memberRepository.save(member);
            run.setStatus(outcome
                    == PromotionCatalogService.AutomationCompensationOutcome.ANOMALY_REVIEW_REQUIRED
                    ? AutomationRunStatus.REVIEW_REQUIRED
                    : AutomationRunStatus.CANCELLED);
        }
        run.setCompletedAt(Instant.now());
        run.setUpdatedBy("SYSTEM");
        runRepository.save(run);
    }

    @Transactional
    public IssueJobView createIssueJob(String runPublicId, int requestedBatchSize) {
        PromotionAutomationRun run = requireRun(runPublicId);
        if (run.getStatus() != AutomationRunStatus.AUDIENCE_READY
                && run.getStatus() != AutomationRunStatus.PARTIAL_SUCCESS) {
            throw conflict("Run is not ready for issuance");
        }
        if (jobRepository.existsByRunPublicIdAndStatusIn(runPublicId,
                List.of(IssueJobStatus.PENDING, IssueJobStatus.RUNNING))) {
            throw conflict("Run already has an active issue job");
        }
        PromotionAudienceSnapshot snapshot = snapshotRepository.findByRunPublicId(runPublicId)
                .orElseThrow(() -> conflict("Audience snapshot was not found"));
        PromotionIssueJob job = new PromotionIssueJob();
        job.setRunPublicId(runPublicId);
        job.setSnapshotPublicId(snapshot.getPublicId());
        job.setBatchSize(Math.max(200, Math.min(requestedBatchSize, 500)));
        job.setCreatedBy("SYSTEM");
        job.setUpdatedBy("SYSTEM");
        return jobView(jobRepository.save(job));
    }

    @Transactional
    public void ensureIssueJob(String runPublicId, int requestedBatchSize) {
        if (!jobRepository.existsByRunPublicId(runPublicId)) {
            createIssueJob(runPublicId, requestedBatchSize);
        }
    }

    @Transactional(readOnly = true)
    public List<OpportunityView> opportunities() {
        List<OpportunityView> result = new ArrayList<>();
        PromotionPlaybook birthday = playbookRepository.findByCodeAndDeletedAtIsNull(BIRTHDAY)
                .orElse(null);
        Integer count = null;
        Integer excluded = null;
        String insight = "Nguồn audience sinh nhật chưa sẵn sàng.";
        try {
            LocalDate today = LocalDate.now(BUSINESS_ZONE);
            List<String> users = birthdayClient.findEligible(today, 100_000);
            count = users.size();
            excluded = (int) users.stream().filter(user -> walletRepository.findByIssuanceKey(
                    BIRTHDAY + ":" + user + ":" + today.getYear()).isPresent()).count();
            insight = count + " thành viên đủ điều kiện sinh nhật hôm nay; "
                    + excluded + " người đã nhận quà năm nay sẽ được loại trừ.";
        } catch (BusinessException ignored) {
            // Opportunity stays visible and explicitly reports unavailable source data.
        }
        int recipients = count == null ? 0 : Math.max(0, count - value(excluded));
        if (birthday != null && birthday.getStatus() == PlaybookStatus.ACTIVE
                && recipients > 0) {
            Promotion promotion = promotionRepository
                    .findByPublicIdAndDeletedAtIsNull(birthday.getPromotionPublicId())
                    .orElse(null);
            BigDecimal unitCost = promotion == null
                    ? BigDecimal.ZERO : estimatedUnitCost(promotion);
            result.add(new OpportunityView(
                    BIRTHDAY, "Quà sinh nhật", insight,
                    "Dựa trên ngày sinh trong hồ sơ thành viên; mỗi khách chỉ nhận một lần mỗi năm.",
                    recipients, excluded, unitCost.multiply(BigDecimal.valueOf(recipients)),
                    birthday.getBudgetLimit(), budgetRemaining(birthday),
                    "READY", "Xem danh sách và phát hành",
                    "PLAYBOOK", view(birthday)));
        }
        return result;
    }

    private PromotionAutomationRun createRun(
            PromotionPlaybook playbook, String triggerReference,
            List<String> customerIds, String idempotencyKey,
            java.util.function.Function<String, String> issuanceKeyFactory) {
        PromotionAutomationRun existing = runRepository.findByIdempotencyKey(idempotencyKey)
                .orElse(null);
        if (existing != null) return existing;
        ConfiguredReferences references = requireConfiguredReferences(playbook);
        assertApprovedConfiguration(playbook, references);
        PromotionAutomationRun run = new PromotionAutomationRun();
        run.setPlaybookPublicId(playbook.getPublicId());
        run.setCampaignPublicId(playbook.getCampaignPublicId());
        run.setPromotionPublicId(playbook.getPromotionPublicId());
        run.setPlaybookCode(playbook.getCode());
        run.setPlaybookVersion(playbook.getPlaybookVersion());
        run.setApprovedConfigHash(playbook.getApprovedConfigHash());
        run.setConfigSnapshotJson(playbook.getConfigJson());
        run.setScopeSnapshotJson(playbook.getScopeJson());
        run.setBudgetSnapshot(playbook.getBudgetLimit());
        run.setQuotaSnapshot(quotaView(playbook).effectiveQuota());
        run.setEstimatedUnitCost(estimatedUnitCost(references.promotion()));
        run.setTriggerType(playbook.getTriggerType());
        run.setTriggerReference(triggerReference);
        run.setRunActor("SYSTEM");
        run.setAuthorizedBy(playbook.getApprovedBy());
        run.setIdempotencyKey(idempotencyKey);
        run.setStartedAt(Instant.now());
        run.setCreatedBy("SYSTEM");
        run.setUpdatedBy("SYSTEM");
        run = runRepository.save(run);

        LinkedHashSet<String> uniqueCustomers = new LinkedHashSet<>(customerIds);
        PromotionAudienceSnapshot snapshot = new PromotionAudienceSnapshot();
        snapshot.setRunPublicId(run.getPublicId());
        snapshot.setAudienceRuleSnapshotJson(playbook.getConfigJson());
        snapshot.setTotalCount(uniqueCustomers.size());
        snapshot.setCapturedAt(Instant.now());
        snapshot.setCreatedBy("SYSTEM");
        snapshot.setUpdatedBy("SYSTEM");
        snapshot = snapshotRepository.save(snapshot);

        int eligible = 0;
        int excluded = 0;
        List<PromotionAudienceMember> members = new ArrayList<>();
        for (String customer : uniqueCustomers) {
            String issuanceKey = issuanceKeyFactory.apply(customer);
            PromotionAudienceMember member = new PromotionAudienceMember();
            member.setSnapshotPublicId(snapshot.getPublicId());
            member.setRunPublicId(run.getPublicId());
            member.setCustomerPublicId(customer);
            member.setIssuanceKey(issuanceKey);
            if (walletRepository.findByIssuanceKey(issuanceKey).isPresent()) {
                member.setStatus(AudienceMemberStatus.SKIPPED_ALREADY_GRANTED);
                member.setReasonCode("IDEMPOTENCY_KEY_ALREADY_GRANTED");
                excluded++;
            } else {
                eligible++;
            }
            member.setCreatedBy("SYSTEM");
            member.setUpdatedBy("SYSTEM");
            members.add(member);
        }
        memberRepository.saveAll(members);
        snapshot.setEligibleCount(eligible);
        snapshot.setExcludedCount(excluded);
        snapshotRepository.save(snapshot);
        run.setAudienceCount(uniqueCustomers.size());
        run.setSkippedCount(excluded);
        run.setStatus(AutomationRunStatus.AUDIENCE_READY);
        run = runRepository.save(run);
        auditTrailService.record("PROMOTION_AUTOMATION_RUN", run.getPublicId(),
                "AUTOMATION_RUN_CREATE", null,
                Map.of(
                        "playbookCode", run.getPlaybookCode(),
                        "playbookVersion", run.getPlaybookVersion(),
                        "approvedConfigHash", run.getApprovedConfigHash(),
                        "authorizedBy", run.getAuthorizedBy(),
                        "runActor", run.getRunActor(),
                        "audienceCount", run.getAudienceCount(),
                        "eligibleCount", eligible,
                        "excludedCount", excluded),
                "SYSTEM");
        return run;
    }

    private ConfiguredReferences requireConfiguredReferences(PromotionPlaybook playbook) {
        if (playbook.getCampaignPublicId() == null || playbook.getPromotionPublicId() == null) {
            throw conflict("Playbook needs an approved campaign and a promotion benefit");
        }
        PromotionCampaign campaign = campaignRepository.findByPublicId(playbook.getCampaignPublicId())
                .orElseThrow(() -> conflict("Configured campaign was not found"));
        Promotion promotion = promotionRepository.findByPublicIdAndDeletedAtIsNull(
                        playbook.getPromotionPublicId())
                .orElseThrow(() -> conflict("Configured promotion was not found"));
        if (!Objects.equals(promotion.getCampaignPublicId(), campaign.getPublicId())) {
            throw conflict("Configured promotion does not belong to the campaign");
        }
        if (campaign.getApprovalStatus() != CampaignApprovalStatus.APPROVED) {
            throw conflict("Playbook campaign must be approved before activation");
        }
        if (promotion.getPromotionType() == PromotionType.AUTO) {
            throw conflict("Playbook issuance requires a voucher or coupon stored in the customer wallet");
        }
        return new ConfiguredReferences(campaign, promotion);
    }

    private PromotionPlaybook activeByCode(String code) {
        PromotionPlaybook playbook = playbookRepository.findByCodeAndStatusAndDeletedAtIsNull(
                        code, PlaybookStatus.ACTIVE)
                .orElseThrow(() -> conflict("Playbook " + code + " is not active"));
        assertApprovedConfiguration(playbook, requireConfiguredReferences(playbook));
        return playbook;
    }

    private PromotionPlaybook requirePlaybook(String publicId) {
        return playbookRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> new BusinessException(
                        "PLAYBOOK_NOT_FOUND", "Playbook was not found", HttpStatus.NOT_FOUND));
    }

    private PromotionAutomationRun requireRun(String publicId) {
        return runRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException(
                        "AUTOMATION_RUN_NOT_FOUND", "Automation run was not found",
                        HttpStatus.NOT_FOUND));
    }

    private void validateJson(String value, String field) {
        try { objectMapper.readTree(value); }
        catch (Exception exception) {
            throw new BusinessException("INVALID_AUTOMATION_JSON",
                    field + " must be valid JSON", HttpStatus.BAD_REQUEST);
        }
    }

    private PlaybookView view(PromotionPlaybook item) {
        List<PromotionAutomationRun> runs = runRepository
                .findByPlaybookPublicIdOrderByCreatedAtDesc(item.getPublicId());
        return new PlaybookView(item.getPublicId(), item.getVersion(), item.getCode(),
                item.getName(), item.getDescription(), item.getStatus(),
                item.getPlaybookVersion(), item.getTriggerType(),
                item.getCampaignPublicId(), item.getPromotionPublicId(),
                item.getConfigJson(), item.getScopeJson(), item.getBudgetLimit(),
                item.getQuotaLimit(), item.getSubmittedBy(), item.getSubmittedAt(),
                item.getApprovedBy(), item.getApprovedAt(), item.getConfigHash(),
                item.getSubmittedPlaybookVersion(), item.getSubmittedConfigHash(),
                item.getApprovedPlaybookVersion(), item.getApprovedConfigHash(),
                item.getBudgetPeriodKey(), money(item.getBudgetCommitted()),
                budgetRemaining(item), value(item.getQuotaCommitted()),
                actorDirectory.displayName(item.getSubmittedBy()),
                actorDirectory.displayName(item.getApprovedBy()),
                quotaView(item), entitlementSummary(item, runs),
                item.getUpdatedBy(), item.getUpdatedAt());
    }

    private RunView view(PromotionAutomationRun item) {
        List<IssueJobView> jobs = jobRepository
                .findByRunPublicIdOrderByCreatedAtDesc(item.getPublicId())
                .stream().map(this::jobView).toList();
        PromotionAudienceSnapshot snapshot = snapshotRepository
                .findByRunPublicId(item.getPublicId()).orElse(null);
        List<PromotionAudienceMember> members = memberRepository
                .findByRunPublicIdOrderByIdAsc(item.getPublicId());
        Map<String, Long> reasons = new LinkedHashMap<>();
        members.stream().filter(member -> member.getReasonCode() != null)
                .forEach(member -> reasons.merge(member.getReasonCode(), 1L, Long::sum));
        int retrying = (int) members.stream()
                .filter(member -> member.getStatus() == AudienceMemberStatus.FAILED_RETRYABLE)
                .count();
        BigDecimal committedCost = members.stream()
                .map(PromotionAudienceMember::getBudgetReservedAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new RunView(item.getPublicId(), item.getPlaybookCode(),
                item.getPlaybookVersion(), item.getTriggerType(),
                item.getTriggerReference(), item.getRunActor(),
                actorDirectory.displayName(item.getRunActor()), item.getAuthorizedBy(),
                actorDirectory.displayName(item.getAuthorizedBy()), item.getIdempotencyKey(),
                item.getStatus(), item.getAudienceCount(),
                item.getIssuedCount(), item.getSkippedCount(), item.getFailedCount(),
                item.getStartedAt(), item.getCompletedAt(), item.getApprovedConfigHash(),
                item.getEstimatedUnitCost(), item.getConfigSnapshotJson(),
                snapshot == null ? null : snapshot.getPublicId(),
                snapshot == null ? 0 : snapshot.getEligibleCount(),
                snapshot == null ? 0 : snapshot.getExcludedCount(), retrying,
                money(committedCost),
                reasons.entrySet().stream()
                        .map(entry -> new ReasonCount(entry.getKey(), entry.getValue()))
                        .toList(),
                members.stream().map(member -> new AudienceMemberView(
                        member.getPublicId(), member.getCustomerPublicId(), member.getStatus(),
                        member.getReasonCode(), member.getAttemptCount(),
                        member.getWalletPublicId(), member.getIssuanceKey(),
                        money(member.getBudgetReservedAmount()))).toList(),
                jobs);
    }

    private QuotaView quotaView(PromotionPlaybook playbook) {
        Integer requested = playbook.getQuotaLimit();
        int committed = currentPeriod(playbook) ? value(playbook.getQuotaCommitted()) : 0;
        Integer playbookRemaining = requested == null
                ? null : Math.max(0, requested - committed);
        BigDecimal unitCost = null;
        Integer budgetQuota = null;
        Integer campaignQuota = null;
        Integer promotionQuota = null;
        try {
            if (playbook.getPromotionPublicId() != null) {
                Promotion promotion = promotionRepository
                        .findByPublicIdAndDeletedAtIsNull(playbook.getPromotionPublicId())
                        .orElse(null);
                if (promotion != null) {
                    unitCost = estimatedUnitCost(promotion);
                    if (playbook.getBudgetLimit() != null) {
                        budgetQuota = wholeUnits(budgetRemaining(playbook), unitCost);
                    }
                    if (promotion.getMaxRedemptions() != null) {
                        long issued = walletRepository
                                .countByPromotionPublicIdAndDeletedAtIsNull(promotion.getPublicId());
                        promotionQuota = remaining(promotion.getMaxRedemptions(), issued);
                    }
                }
            }
            if (playbook.getCampaignPublicId() != null) {
                PromotionCampaign campaign = campaignRepository
                        .findByPublicIdAndDeletedAtIsNull(playbook.getCampaignPublicId())
                        .orElse(null);
                if (campaign != null && campaign.getMaxRedemptions() != null) {
                    long issued = walletRepository.countByCampaignPublicId(campaign.getPublicId());
                    long consumedOrIssued = Math.max(issued, value(campaign.getRedemptionCount()));
                    campaignQuota = remaining(campaign.getMaxRedemptions(), consumedOrIssued);
                }
            }
        } catch (BusinessException ignored) {
            // Drafts can be incomplete. Submission still performs strict validation.
        }

        LinkedHashMap<String, Integer> limits = new LinkedHashMap<>();
        if (playbookRemaining != null) limits.put("PLAYBOOK_QUOTA", playbookRemaining);
        if (budgetQuota != null) limits.put("MONTHLY_BUDGET", budgetQuota);
        if (campaignQuota != null) limits.put("CAMPAIGN_QUOTA", campaignQuota);
        if (promotionQuota != null) limits.put("BENEFIT_QUOTA", promotionQuota);
        Map.Entry<String, Integer> effective = limits.entrySet().stream()
                .min(Map.Entry.comparingByValue()).orElse(null);
        return new QuotaView(unitCost, requested, playbookRemaining, budgetQuota,
                campaignQuota, promotionQuota,
                effective == null ? null : effective.getValue(),
                effective == null ? null : effective.getKey());
    }

    private EntitlementSummary entitlementSummary(
            PromotionPlaybook playbook, List<PromotionAutomationRun> runs) {
        return entitlementSummary(runs, budgetRemaining(playbook));
    }

    private EntitlementSummary aggregateEntitlements(
            List<PromotionPlaybook> playbooks, BigDecimal campaignBudget) {
        List<PromotionAutomationRun> runs = playbooks.stream()
                .flatMap(playbook -> runRepository
                        .findByPlaybookPublicIdOrderByCreatedAtDesc(playbook.getPublicId())
                        .stream())
                .toList();
        EntitlementSummary summary = entitlementSummary(runs, null);
        BigDecimal liability = summary.walletIssuedCommitted()
                .add(summary.orderReserved()).add(summary.usedAmount());
        BigDecimal remaining = campaignBudget == null ? null
                : money(campaignBudget.subtract(liability).max(BigDecimal.ZERO));
        return new EntitlementSummary(summary.issued(), summary.unredeemed(),
                summary.reserved(), summary.used(), summary.expiredOrRevoked(),
                summary.walletIssuedCommitted(), summary.orderReserved(),
                summary.usedAmount(), summary.releasedAmount(), remaining);
    }

    private EntitlementSummary entitlementSummary(
            List<PromotionAutomationRun> runs, BigDecimal remainingCapacity) {
        if (runs.isEmpty()) {
            return new EntitlementSummary(0, 0, 0, 0, 0,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, remainingCapacity);
        }
        List<String> runIds = runs.stream().map(PromotionAutomationRun::getPublicId).toList();
        List<PromotionAudienceMember> members = memberRepository.findByRunPublicIdIn(runIds);
        Map<String, BigDecimal> amountByMember = new HashMap<>();
        members.forEach(member -> amountByMember.put(member.getPublicId(),
                money(member.getBudgetReservedAmount())));
        var wallets = walletRepository
                .findByAutomationRunPublicIdInAndDeletedAtIsNull(runIds);
        List<String> walletIds = wallets.stream().map(wallet -> wallet.getPublicId()).toList();
        Set<String> reservedWallets = walletIds.isEmpty() ? Set.of()
                : redemptionRepository
                        .findByUserPromotionPublicIdInAndStatusInAndDeletedAtIsNull(
                                walletIds, EnumSet.of(PromotionRedemptionStatus.RESERVED))
                        .stream().map(redemption -> redemption.getUserPromotionPublicId())
                        .collect(java.util.stream.Collectors.toSet());
        int unredeemed = 0;
        int reserved = 0;
        int used = 0;
        int expiredOrRevoked = 0;
        BigDecimal walletCommitted = BigDecimal.ZERO;
        BigDecimal orderReserved = BigDecimal.ZERO;
        BigDecimal usedAmount = BigDecimal.ZERO;
        BigDecimal released = BigDecimal.ZERO;
        for (var wallet : wallets) {
            BigDecimal amount = amountByMember.getOrDefault(
                    wallet.getAudienceMemberPublicId(), BigDecimal.ZERO);
            if (wallet.getStatus() == UserPromotionStatus.EXPIRED
                    || wallet.getStatus() == UserPromotionStatus.REVOKED) {
                expiredOrRevoked++;
                released = released.add(amount);
            } else if (wallet.getStatus() == UserPromotionStatus.USED
                    || value(wallet.getUsageCount()) > 0) {
                used++;
                usedAmount = usedAmount.add(amount);
            } else if (reservedWallets.contains(wallet.getPublicId())) {
                reserved++;
                orderReserved = orderReserved.add(amount);
            } else {
                unredeemed++;
                walletCommitted = walletCommitted.add(amount);
            }
        }
        return new EntitlementSummary(wallets.size(), unredeemed, reserved, used,
                expiredOrRevoked, money(walletCommitted), money(orderReserved),
                money(usedAmount), money(released), remainingCapacity);
    }

    private IssueJobView jobView(PromotionIssueJob item) {
        return new IssueJobView(item.getPublicId(), item.getStatus(), item.getBatchSize(),
                item.getProcessedCount(), item.getIssuedCount(), item.getSkippedCount(),
                item.getFailedCount(), item.getLastError(), item.getStartedAt(),
                item.getCompletedAt());
    }

    private void assertApprovedConfiguration(
            PromotionPlaybook playbook, ConfiguredReferences references) {
        String currentHash = configurationHash(
                playbook, references.campaign(), references.promotion());
        if (!Objects.equals(playbook.getApprovedPlaybookVersion(),
                playbook.getPlaybookVersion())
                || !Objects.equals(playbook.getApprovedConfigHash(), currentHash)) {
            throw conflict("Phiên bản hoặc cấu hình hiện tại không khớp với bản đã duyệt");
        }
        estimatedUnitCost(references.promotion());
    }

    private String configurationHash(
            PromotionPlaybook playbook,
            PromotionCampaign campaign,
            Promotion promotion) {
        try {
            Map<String, Object> document = new TreeMap<>();
            document.put("playbookCode", playbook.getCode());
            document.put("playbookVersion", playbook.getPlaybookVersion());
            document.put("triggerType", playbook.getTriggerType());
            document.put("campaignPublicId", playbook.getCampaignPublicId());
            document.put("promotionPublicId", playbook.getPromotionPublicId());
            document.put("config", canonicalValue(
                    objectMapper.readTree(playbook.getConfigJson())));
            document.put("scope", canonicalValue(
                    objectMapper.readTree(playbook.getScopeJson())));
            document.put("budgetLimit", playbook.getBudgetLimit());
            document.put("quotaLimit", playbook.getQuotaLimit());
            if (campaign != null) {
                Map<String, Object> targetCampaign = new TreeMap<>();
                targetCampaign.put("scopeType", campaign.getScopeType());
                targetCampaign.put("cinemaScope", canonicalJson(campaign.getCinemaScopeJson()));
                targetCampaign.put("startAt", campaign.getStartAt());
                targetCampaign.put("endAt", campaign.getEndAt());
                targetCampaign.put("budgetAmount", campaign.getBudgetAmount());
                targetCampaign.put("maxRedemptions", campaign.getMaxRedemptions());
                targetCampaign.put("maxRedemptionsPerUser",
                        campaign.getMaxRedemptionsPerUser());
                document.put("campaign", targetCampaign);
            }
            if (promotion != null) {
                Map<String, Object> benefit = new TreeMap<>();
                benefit.put("type", promotion.getPromotionType());
                benefit.put("conditions", canonicalJson(promotion.getConditionsJson()));
                benefit.put("actions", canonicalJson(promotion.getActionsJson()));
                benefit.put("metadata", canonicalJson(promotion.getMetadataJson()));
                benefit.put("maxRedemptions", promotion.getMaxRedemptions());
                benefit.put("maxRedemptionsPerUser",
                        promotion.getMaxRedemptionsPerUser());
                benefit.put("validFrom", promotion.getValidFrom());
                benefit.put("validTo", promotion.getValidTo());
                document.put("benefit", benefit);
            }
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(objectMapper.writeValueAsBytes(document));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not hash playbook configuration", exception);
        }
    }

    private Object canonicalJson(String value) throws Exception {
        return value == null || value.isBlank()
                ? null : canonicalValue(objectMapper.readTree(value));
    }

    private Object canonicalValue(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isObject()) {
            Map<String, Object> object = new TreeMap<>();
            node.fields().forEachRemaining(entry -> object.put(
                    entry.getKey(), canonicalValue(entry.getValue())));
            return object;
        }
        if (node.isArray()) {
            List<Object> array = new ArrayList<>();
            node.forEach(item -> array.add(canonicalValue(item)));
            return array;
        }
        if (node.isNumber()) return node.decimalValue();
        if (node.isBoolean()) return node.booleanValue();
        return node.asText();
    }

    private BigDecimal estimatedUnitCost(Promotion promotion) {
        try {
            JsonNode action = objectMapper.readTree(promotion.getActionsJson());
            if (action.isArray()) action = action.isEmpty() ? action : action.get(0);
            String type = text(action,
                    "discountType", "type", "actionType").toUpperCase(Locale.ROOT);
            BigDecimal value = switch (type) {
                case "FIXED", "FIXED_AMOUNT" -> decimal(action,
                        "discountValue", "value", "amount");
                case "PERCENT", "PERCENTAGE" -> decimal(action,
                        "maxDiscountAmount", "maximumDiscountAmount", "maxAmount");
                default -> BigDecimal.ZERO;
            };
            if (value == null || value.signum() <= 0) {
                throw conflict("Quyền lợi tự động phải có mức giảm tiền tối đa để bảo vệ ngân sách");
            }
            return money(value);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw conflict("Không thể xác định chi phí tối đa của quyền lợi tự động");
        }
    }

    private String text(JsonNode node, String... fields) {
        if (node == null) return "";
        for (String field : fields) {
            if (node.hasNonNull(field) && !node.path(field).asText().isBlank()) {
                return node.path(field).asText();
            }
        }
        return "";
    }

    private BigDecimal decimal(JsonNode node, String... fields) {
        if (node == null) return null;
        for (String field : fields) {
            if (node.hasNonNull(field) && node.path(field).isNumber()) {
                return node.path(field).decimalValue();
            }
        }
        return null;
    }

    private BigDecimal budgetRemaining(PromotionPlaybook playbook) {
        if (playbook.getBudgetLimit() == null) return null;
        BigDecimal committedThisMonth = currentPeriod(playbook)
                ? money(playbook.getBudgetCommitted())
                : BigDecimal.ZERO;
        return money(playbook.getBudgetLimit()
                .subtract(committedThisMonth)
                .max(BigDecimal.ZERO));
    }

    private boolean currentPeriod(PromotionPlaybook playbook) {
        return YearMonth.now(BUSINESS_ZONE).toString()
                .equals(playbook.getBudgetPeriodKey());
    }

    private int wholeUnits(BigDecimal amount, BigDecimal unitCost) {
        if (amount == null || unitCost == null || unitCost.signum() <= 0) return 0;
        BigDecimal units = amount.divideToIntegralValue(unitCost);
        return units.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0
                ? Integer.MAX_VALUE : Math.max(0, units.intValue());
    }

    private int remaining(Integer ceiling, long committed) {
        if (ceiling == null) return Integer.MAX_VALUE;
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE,
                ceiling.longValue() - committed));
    }

    private Map<String, Object> playbookAudit(PromotionPlaybook playbook) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("code", playbook.getCode());
        value.put("status", playbook.getStatus());
        value.put("playbookVersion", playbook.getPlaybookVersion());
        value.put("campaignPublicId", playbook.getCampaignPublicId());
        value.put("promotionPublicId", playbook.getPromotionPublicId());
        value.put("configHash", playbook.getConfigHash());
        value.put("submittedPlaybookVersion", playbook.getSubmittedPlaybookVersion());
        value.put("submittedConfigHash", playbook.getSubmittedConfigHash());
        value.put("approvedPlaybookVersion", playbook.getApprovedPlaybookVersion());
        value.put("approvedConfigHash", playbook.getApprovedConfigHash());
        value.put("budgetLimit", playbook.getBudgetLimit());
        value.put("quotaLimit", playbook.getQuotaLimit());
        return value;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(
                2, java.math.RoundingMode.HALF_UP);
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private record ConfiguredReferences(
            PromotionCampaign campaign, Promotion promotion) { }

    private BusinessException conflict(String message) {
        return new BusinessException("AUTOMATION_CONFLICT", message, HttpStatus.CONFLICT);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
