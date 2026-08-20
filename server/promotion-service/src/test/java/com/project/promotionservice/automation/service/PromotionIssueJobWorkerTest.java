package com.project.promotionservice.automation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.automation.entity.PromotionAudienceMember;
import com.project.promotionservice.automation.entity.PromotionAutomationRun;
import com.project.promotionservice.automation.entity.PromotionIssueJob;
import com.project.promotionservice.automation.enums.AudienceMemberStatus;
import com.project.promotionservice.automation.enums.AutomationRunStatus;
import com.project.promotionservice.automation.enums.IssueJobStatus;
import com.project.promotionservice.automation.repository.PromotionAudienceMemberRepository;
import com.project.promotionservice.automation.repository.PromotionAutomationRunRepository;
import com.project.promotionservice.automation.repository.PromotionIssueJobRepository;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import com.project.promotionservice.promotion.service.PromotionCatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionIssueJobWorkerTest {

    @Mock private PromotionIssueJobRepository jobRepository;
    @Mock private PromotionAutomationRunRepository runRepository;
    @Mock private PromotionAudienceMemberRepository memberRepository;
    @Mock private PromotionCampaignRepository campaignRepository;
    @Mock private PromotionCatalogService catalogService;
    @Mock private PromotionAutomationBudgetService budgetService;

    private PromotionIssueJobWorker worker;

    @BeforeEach
    void setUp() {
        worker = new PromotionIssueJobWorker(
                jobRepository, runRepository, memberRepository,
                campaignRepository, catalogService, budgetService,
                new ObjectMapper());
    }

    @Test
    void reloadsMemberAfterRequiresNewBudgetReservationBeforeSavingIssuance() {
        when(jobRepository.save(any(PromotionIssueJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(runRepository.save(any(PromotionAutomationRun.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(memberRepository.save(any(PromotionAudienceMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        PromotionAutomationRun run = new PromotionAutomationRun();
        run.setPublicId("run-1");
        run.setPlaybookPublicId("playbook-1");
        run.setCampaignPublicId("campaign-1");
        run.setPromotionPublicId("promotion-1");
        run.setConfigSnapshotJson("{\"validityDays\":30}");
        run.setEstimatedUnitCost(new BigDecimal("50000"));
        run.setQuotaSnapshot(100);

        PromotionIssueJob job = new PromotionIssueJob();
        job.setPublicId("job-1");
        job.setRunPublicId("run-1");
        job.setStatus(IssueJobStatus.RUNNING);

        PromotionAudienceMember stale = member(1L, 0);
        PromotionAudienceMember freshAfterReservation = member(1L, 1);
        freshAfterReservation.setBudgetReservedAmount(new BigDecimal("50000"));
        freshAfterReservation.setBudgetPeriodKey("2026-08");

        PromotionCampaign campaign = new PromotionCampaign();
        campaign.setBudgetRemaining(new BigDecimal("5000000"));

        when(runRepository.findByPublicId("run-1")).thenReturn(Optional.of(run));
        when(campaignRepository.findByPublicId("campaign-1"))
                .thenReturn(Optional.of(campaign));
        when(memberRepository.findByRunPublicIdAndStatusOrderByIdAsc(
                eq("run-1"), eq(AudienceMemberStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(stale), List.of());
        when(memberRepository.findByRunPublicIdAndStatusAndAttemptCountLessThanOrderByIdAsc(
                eq("run-1"), eq(AudienceMemberStatus.FAILED_RETRYABLE), eq(3),
                any(Pageable.class))).thenReturn(List.of());
        when(budgetService.reserveForMember(
                "member-1", "playbook-1", new BigDecimal("50000")))
                .thenReturn(PromotionAutomationBudgetService.ReservationResult.RESERVED);
        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(freshAfterReservation));
        when(catalogService.issueFromAutomation(
                "promotion-1", "customer-1", "birthday:customer-1:2026",
                "run-1", "member-1", 30))
                .thenReturn(new PromotionCatalogService.AutomationIssueOutcome(
                        true, false, "wallet-1", true));
        when(memberRepository.countByRunPublicIdAndStatus(
                "run-1", AudienceMemberStatus.ISSUED)).thenReturn(1L);

        worker.process(job);

        assertThat(freshAfterReservation.getStatus())
                .isEqualTo(AudienceMemberStatus.ISSUED);
        assertThat(freshAfterReservation.getWalletPublicId()).isEqualTo("wallet-1");
        assertThat(job.getProcessedCount()).isEqualTo(1);
        assertThat(job.getIssuedCount()).isEqualTo(1);
        assertThat(run.getIssuedCount()).isEqualTo(1);
        assertThat(run.getStatus()).isEqualTo(AutomationRunStatus.COMPLETED);
        verify(catalogService).issueFromAutomation(
                "promotion-1", "customer-1", "birthday:customer-1:2026",
                "run-1", "member-1", 30);
    }

    @Test
    void processNextContinuesWithMergedRunningJobReturnedByRepository() {
        when(jobRepository.save(any(PromotionIssueJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(runRepository.save(any(PromotionAutomationRun.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        PromotionIssueJob pending = new PromotionIssueJob();
        pending.setPublicId("job-pending");
        pending.setRunPublicId("run-empty");
        pending.setStatus(IssueJobStatus.PENDING);

        PromotionIssueJob merged = new PromotionIssueJob();
        merged.setPublicId("job-pending");
        merged.setRunPublicId("run-empty");
        merged.setStatus(IssueJobStatus.RUNNING);
        merged.setVersion(1);

        PromotionAutomationRun run = new PromotionAutomationRun();
        run.setPublicId("run-empty");

        when(jobRepository.findFirstByStatusOrderByCreatedAtAsc(IssueJobStatus.PENDING))
                .thenReturn(Optional.of(pending));
        when(jobRepository.save(same(pending))).thenReturn(merged);
        when(runRepository.findByPublicId("run-empty")).thenReturn(Optional.of(run));
        when(memberRepository.findByRunPublicIdAndStatusOrderByIdAsc(
                eq("run-empty"), eq(AudienceMemberStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of());
        when(memberRepository.findByRunPublicIdAndStatusAndAttemptCountLessThanOrderByIdAsc(
                eq("run-empty"), eq(AudienceMemberStatus.FAILED_RETRYABLE), eq(3),
                any(Pageable.class))).thenReturn(List.of());

        worker.processNext();

        assertThat(pending.getStatus()).isEqualTo(IssueJobStatus.RUNNING);
        assertThat(merged.getStatus()).isEqualTo(IssueJobStatus.COMPLETED);
        assertThat(run.getStatus()).isEqualTo(AutomationRunStatus.COMPLETED);
    }

    private PromotionAudienceMember member(Long id, int version) {
        PromotionAudienceMember member = new PromotionAudienceMember();
        member.setId(id);
        member.setVersion(version);
        member.setPublicId("member-1");
        member.setRunPublicId("run-1");
        member.setSnapshotPublicId("snapshot-1");
        member.setCustomerPublicId("customer-1");
        member.setIssuanceKey("birthday:customer-1:2026");
        return member;
    }
}
