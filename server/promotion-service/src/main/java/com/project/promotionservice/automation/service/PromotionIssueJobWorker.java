package com.project.promotionservice.automation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.automation.entity.*;
import com.project.promotionservice.automation.enums.*;
import com.project.promotionservice.automation.repository.*;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import com.project.promotionservice.promotion.service.PromotionCatalogService;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class PromotionIssueJobWorker {
    private final PromotionIssueJobRepository jobRepository;
    private final PromotionAutomationRunRepository runRepository;
    private final PromotionAudienceMemberRepository memberRepository;
    private final PromotionCampaignRepository campaignRepository;
    private final PromotionCatalogService catalogService;
    private final PromotionAutomationBudgetService budgetService;
    private final ObjectMapper objectMapper;

    public PromotionIssueJobWorker(
            PromotionIssueJobRepository jobRepository,
            PromotionAutomationRunRepository runRepository,
            PromotionAudienceMemberRepository memberRepository,
            PromotionCampaignRepository campaignRepository,
            PromotionCatalogService catalogService,
            PromotionAutomationBudgetService budgetService,
            ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.runRepository = runRepository;
        this.memberRepository = memberRepository;
        this.campaignRepository = campaignRepository;
        this.catalogService = catalogService;
        this.budgetService = budgetService;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${promotion.automation.issue-worker-delay-ms:2000}")
    public void processNext() {
        PromotionIssueJob job = jobRepository
                .findFirstByStatusOrderByCreatedAtAsc(IssueJobStatus.PENDING)
                .orElseGet(() -> jobRepository
                        .findFirstByStatusOrderByCreatedAtAsc(IssueJobStatus.RUNNING)
                        .orElse(null));
        if (job == null) return;
        if (job.getStatus() == IssueJobStatus.PENDING) {
            job.setStatus(IssueJobStatus.RUNNING);
            job.setStartedAt(Instant.now());
            job.setUpdatedBy("SYSTEM");
            // save() may merge a detached scheduler entity. Continue with the
            // returned instance so its incremented optimistic-lock version is used.
            job = jobRepository.save(job);
        }
        process(job);
    }

    void process(PromotionIssueJob job) {
        PromotionAutomationRun run = runRepository.findByPublicId(job.getRunPublicId())
                .orElseThrow();
        run.setStatus(AutomationRunStatus.ISSUING);
        run = runRepository.save(run);
        while (true) {
            var batch = memberRepository.findByRunPublicIdAndStatusOrderByIdAsc(
                    run.getPublicId(), AudienceMemberStatus.PENDING,
                    PageRequest.of(0, job.getBatchSize()));
            if (batch.isEmpty()) {
                batch = memberRepository
                        .findByRunPublicIdAndStatusAndAttemptCountLessThanOrderByIdAsc(
                                run.getPublicId(), AudienceMemberStatus.FAILED_RETRYABLE,
                                3, PageRequest.of(0, job.getBatchSize()));
            }
            if (batch.isEmpty()) break;

            PromotionCampaign campaign = campaignRepository
                    .findByPublicId(run.getCampaignPublicId()).orElseThrow();
            boolean budgetExhausted = campaign.getBudgetRemaining() == null
                    || campaign.getBudgetRemaining().compareTo(BigDecimal.ZERO) <= 0;
            for (PromotionAudienceMember member : batch) {
                boolean releaseReservedBudget = false;
                if (budgetExhausted || quotaReached(run)) {
                    member.setStatus(AudienceMemberStatus.SKIPPED_INELIGIBLE);
                    member.setReasonCode(budgetExhausted
                            ? "BUDGET_EXHAUSTED" : "PLAYBOOK_QUOTA_EXHAUSTED");
                    job.setSkippedCount(job.getSkippedCount() + 1);
                } else {
                    PromotionAutomationBudgetService.ReservationResult reservation =
                            budgetService.reserveForMember(member.getPublicId(),
                                    run.getPlaybookPublicId(), run.getEstimatedUnitCost());
                    if (reservation == PromotionAutomationBudgetService.ReservationResult.RESERVED) {
                        // reserveForMember runs in REQUIRES_NEW and increments the
                        // member's optimistic-lock version. Reload before saving the
                        // issuance outcome so a successful grant cannot be retried and
                        // mislabeled as "already granted" by the next worker tick.
                        member = memberRepository.findById(member.getId()).orElseThrow();
                        releaseReservedBudget = issue(run, job, member);
                    } else {
                        member.setStatus(AudienceMemberStatus.SKIPPED_INELIGIBLE);
                        member.setReasonCode(switch (reservation) {
                            case BUDGET_EXHAUSTED -> "PLAYBOOK_MONTHLY_BUDGET_EXHAUSTED";
                            case QUOTA_EXHAUSTED -> "PLAYBOOK_MONTHLY_QUOTA_EXHAUSTED";
                            case PLAYBOOK_INACTIVE -> "PLAYBOOK_NO_LONGER_ACTIVE";
                            default -> "AUTOMATION_RESERVATION_REJECTED";
                        });
                        job.setSkippedCount(job.getSkippedCount() + 1);
                    }
                }
                member.setUpdatedBy("SYSTEM");
                memberRepository.save(member);
                if (releaseReservedBudget) {
                    // Release only after persisting the terminal member status. The
                    // release transaction also updates this member's reservation
                    // fields, so no stale entity is saved afterwards.
                    budgetService.releaseForMember(
                            member.getPublicId(), run.getPlaybookPublicId());
                }
                job.setProcessedCount(job.getProcessedCount() + 1);
            }
            refreshRunCounts(run);
            job.setUpdatedBy("SYSTEM");
            job = jobRepository.save(job);
            run = runRepository.save(run);
        }
        finish(job, run);
    }

    private boolean issue(
            PromotionAutomationRun run,
            PromotionIssueJob job,
            PromotionAudienceMember member) {
        member.setAttemptCount(member.getAttemptCount() + 1);
        try {
            PromotionCatalogService.AutomationIssueOutcome outcome =
                    catalogService.issueFromAutomation(
                            run.getPromotionPublicId(), member.getCustomerPublicId(),
                            member.getIssuanceKey(), run.getPublicId(), member.getPublicId(),
                            validityDays(run));
            member.setWalletPublicId(outcome.walletPublicId());
            if (outcome.issued()) {
                member.setStatus(AudienceMemberStatus.ISSUED);
                member.setReasonCode(null);
                job.setIssuedCount(job.getIssuedCount() + 1);
                run.setIssuedCount(run.getIssuedCount() + 1);
            } else {
                member.setStatus(AudienceMemberStatus.SKIPPED_ALREADY_GRANTED);
                member.setReasonCode("IDEMPOTENCY_KEY_ALREADY_GRANTED");
                job.setSkippedCount(job.getSkippedCount() + 1);
                if (!outcome.budgetCommitted()) {
                    return true;
                }
            }
            return false;
        } catch (Exception exception) {
            boolean finalFailure = member.getAttemptCount() >= 3;
            member.setStatus(finalFailure
                    ? AudienceMemberStatus.FAILED_FINAL
                    : AudienceMemberStatus.FAILED_RETRYABLE);
            member.setReasonCode(finalFailure ? "ISSUANCE_FAILED_FINAL" : "ISSUANCE_RETRY");
            job.setLastError(truncate(exception.getMessage()));
            if (finalFailure) {
                job.setFailedCount(job.getFailedCount() + 1);
                return true;
            }
            return false;
        }
    }

    private Integer validityDays(PromotionAutomationRun run) {
        try {
            int value = objectMapper.readTree(run.getConfigSnapshotJson())
                    .path("validityDays").asInt(0);
            return value > 0 ? value : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean quotaReached(PromotionAutomationRun run) {
        return run.getQuotaSnapshot() != null
                && run.getIssuedCount() >= run.getQuotaSnapshot();
    }

    private void refreshRunCounts(PromotionAutomationRun run) {
        int issued = (int) memberRepository.countByRunPublicIdAndStatus(
                run.getPublicId(), AudienceMemberStatus.ISSUED);
        int skipped = (int) (memberRepository.countByRunPublicIdAndStatus(
                run.getPublicId(), AudienceMemberStatus.SKIPPED_ALREADY_GRANTED)
                + memberRepository.countByRunPublicIdAndStatus(
                run.getPublicId(), AudienceMemberStatus.SKIPPED_INELIGIBLE)
                + memberRepository.countByRunPublicIdAndStatus(
                run.getPublicId(), AudienceMemberStatus.REVOCATION_PENDING));
        int failed = (int) memberRepository.countByRunPublicIdAndStatus(
                run.getPublicId(), AudienceMemberStatus.FAILED_FINAL)
                + (int) memberRepository.countByRunPublicIdAndStatus(
                run.getPublicId(), AudienceMemberStatus.ANOMALY_REVIEW_REQUIRED);
        run.setIssuedCount(issued);
        run.setSkippedCount(skipped);
        run.setFailedCount(failed);
        run.setUpdatedBy("SYSTEM");
    }

    private void finish(PromotionIssueJob job, PromotionAutomationRun run) {
        refreshRunCounts(run);
        boolean failed = run.getFailedCount() > 0;
        boolean issued = run.getIssuedCount() > 0;
        job.setStatus(failed
                ? (issued ? IssueJobStatus.PARTIAL_SUCCESS : IssueJobStatus.FAILED)
                : IssueJobStatus.COMPLETED);
        run.setStatus(failed
                ? (issued ? AutomationRunStatus.PARTIAL_SUCCESS : AutomationRunStatus.FAILED)
                : AutomationRunStatus.COMPLETED);
        Instant now = Instant.now();
        job.setCompletedAt(now);
        run.setCompletedAt(now);
        job.setUpdatedBy("SYSTEM");
        run.setUpdatedBy("SYSTEM");
        jobRepository.save(job);
        runRepository.save(run);
    }

    private String truncate(String value) {
        if (value == null) return "Unknown issuance failure";
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}
