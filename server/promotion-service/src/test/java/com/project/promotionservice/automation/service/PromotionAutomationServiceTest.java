package com.project.promotionservice.automation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.automation.client.BirthdayAudienceClient;
import com.project.promotionservice.automation.client.AutomationActorDirectoryClient;
import com.project.promotionservice.automation.entity.*;
import com.project.promotionservice.automation.enums.PlaybookStatus;
import com.project.promotionservice.automation.repository.*;
import com.project.promotionservice.common.audit.AuditTrailService;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.promotion.entity.Promotion;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.enums.CampaignApprovalStatus;
import com.project.promotionservice.promotion.enums.PromotionType;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import com.project.promotionservice.promotion.repository.PromotionRepository;
import com.project.promotionservice.promotion.repository.PromotionRedemptionRepository;
import com.project.promotionservice.promotion.repository.UserPromotionRepository;
import com.project.promotionservice.promotion.service.PromotionCatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromotionAutomationServiceTest {
    @Mock PromotionPlaybookRepository playbooks;
    @Mock PromotionAutomationRunRepository runs;
    @Mock PromotionAudienceSnapshotRepository snapshots;
    @Mock PromotionAudienceMemberRepository members;
    @Mock PromotionIssueJobRepository jobs;
    @Mock PromotionAutomationSuppressionRepository suppressions;
    @Mock PromotionCampaignRepository campaigns;
    @Mock PromotionRepository promotions;
    @Mock UserPromotionRepository wallets;
    @Mock PromotionRedemptionRepository redemptions;
    @Mock BirthdayAudienceClient birthdayClient;
    @Mock AutomationActorDirectoryClient actorDirectory;
    @Mock PromotionCatalogService catalogService;
    @Mock PromotionAutomationBudgetService budgetService;
    @Mock AuditTrailService auditTrailService;
    PromotionAutomationService service;

    @BeforeEach
    void setUp() {
        service = new PromotionAutomationService(
                playbooks, runs, snapshots, members, jobs, suppressions, campaigns,
                promotions, wallets, redemptions, birthdayClient, actorDirectory,
                new ObjectMapper(), catalogService,
                budgetService, auditTrailService);
    }

    @Test
    void playbookApprovalEnforcesMakerChecker() {
        PromotionPlaybook playbook = configuredPlaybook();
        when(playbooks.findByPublicIdAndDeletedAtIsNull(playbook.getPublicId()))
                .thenReturn(Optional.of(playbook));
        configuredReferences(playbook);
        when(playbooks.save(playbook)).thenReturn(playbook);
        service.submit(playbook.getPublicId(), "maker-1");

        BusinessException selfApproval = assertThrows(
                BusinessException.class,
                () -> service.approve(playbook.getPublicId(), "maker-1"));
        assertTrue(selfApproval.getMessage().contains("không thể tự phê duyệt"));

        var result = service.approve(playbook.getPublicId(), "checker-2");
        assertEquals(PlaybookStatus.ACTIVE, result.status());
        assertEquals("checker-2", result.approvedBy());
        assertNotNull(result.approvedAt());
    }

    @Test
    void approvalRejectsLinkedPromotionChangedAfterSubmission() {
        PromotionPlaybook playbook = configuredPlaybook();
        PromotionCampaign campaign = new PromotionCampaign();
        campaign.setPublicId(playbook.getCampaignPublicId());
        campaign.setApprovalStatus(CampaignApprovalStatus.APPROVED);
        Promotion promotion = new Promotion();
        promotion.setPublicId(playbook.getPromotionPublicId());
        promotion.setCampaignPublicId(playbook.getCampaignPublicId());
        promotion.setPromotionType(PromotionType.VOUCHER);
        promotion.setConditionsJson("{}");
        promotion.setActionsJson(
                "{\"discountType\":\"FIXED_AMOUNT\",\"discountValue\":50000}");
        promotion.setMetadataJson("{}");
        when(playbooks.findByPublicIdAndDeletedAtIsNull(playbook.getPublicId()))
                .thenReturn(Optional.of(playbook));
        when(campaigns.findByPublicId(playbook.getCampaignPublicId()))
                .thenReturn(Optional.of(campaign));
        when(promotions.findByPublicIdAndDeletedAtIsNull(playbook.getPromotionPublicId()))
                .thenReturn(Optional.of(promotion));
        when(playbooks.save(playbook)).thenReturn(playbook);

        service.submit(playbook.getPublicId(), "maker-1");
        promotion.setActionsJson(
                "{\"discountType\":\"FIXED_AMOUNT\",\"discountValue\":60000}");

        BusinessException changed = assertThrows(
                BusinessException.class,
                () -> service.approve(playbook.getPublicId(), "checker-2"));
        assertTrue(changed.getMessage().contains("thay đổi sau khi gửi duyệt"));
        assertEquals(PlaybookStatus.PENDING_APPROVAL, playbook.getStatus());
    }

    @Test
    void secondBookingRunSnapshotsAuthorizationAndCreatesStableIssuanceKey() {
        PromotionPlaybook playbook = configuredPlaybook();
        playbook.setCode(PromotionAutomationService.SECOND_BOOKING);
        playbook.setConfigJson("{\"validityDays\":21}");
        when(playbooks.findByPublicIdAndDeletedAtIsNull(playbook.getPublicId()))
                .thenReturn(Optional.of(playbook));
        configuredReferences(playbook);
        when(playbooks.save(playbook)).thenReturn(playbook);
        service.submit(playbook.getPublicId(), "maker-1");
        service.approve(playbook.getPublicId(), "checker-2");
        when(playbooks.findByCodeAndStatusAndDeletedAtIsNull(
                PromotionAutomationService.SECOND_BOOKING, PlaybookStatus.ACTIVE))
                .thenReturn(Optional.of(playbook));
        when(runs.findByIdempotencyKey("SECOND_BOOKING_INCENTIVE:42"))
                .thenReturn(Optional.empty());
        when(runs.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(snapshots.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(members.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(wallets.findByIssuanceKey(any())).thenReturn(Optional.empty());

        PromotionAutomationRun run = service.createSecondBookingRun("42", "booking-1");

        assertEquals("SYSTEM", run.getRunActor());
        assertEquals("checker-2", run.getAuthorizedBy());
        assertEquals("{\"validityDays\":21}", run.getConfigSnapshotJson());
        assertEquals("SECOND_BOOKING_INCENTIVE:42", run.getIdempotencyKey());
        ArgumentCaptor<List<PromotionAudienceMember>> captor = ArgumentCaptor.forClass(List.class);
        verify(members).saveAll(captor.capture());
        assertEquals("SECOND_BOOKING_INCENTIVE:42",
                captor.getValue().getFirst().getIssuanceKey());
    }

    @Test
    void refundTombstonePreventsOutOfOrderConfirmationFromGrantingBenefit() {
        when(suppressions.existsByPlaybookCodeAndTriggerReference(
                PromotionAutomationService.SECOND_BOOKING, "booking-refunded"))
                .thenReturn(true);

        PromotionAutomationRun run = service.createSecondBookingRun(
                "42", "booking-refunded");

        assertNull(run);
        verifyNoInteractions(playbooks);
    }

    @Test
    void repeatedConfirmationCannotCreateASecondIssueJob() {
        when(jobs.existsByRunPublicId("run-1")).thenReturn(true);

        service.ensureIssueJob("run-1", 200);

        verify(jobs, never()).save(any());
    }

    private PromotionPlaybook configuredPlaybook() {
        PromotionPlaybook playbook = new PromotionPlaybook();
        playbook.setCode("TEST_PLAYBOOK");
        playbook.setName("Test playbook");
        playbook.setTriggerType("BOOKING_CONFIRMED");
        playbook.setCampaignPublicId("11111111-1111-1111-1111-111111111111");
        playbook.setPromotionPublicId("22222222-2222-2222-2222-222222222222");
        playbook.setConfigJson("{}");
        playbook.setScopeJson("{}");
        playbook.setPlaybookVersion(3);
        return playbook;
    }

    private void configuredReferences(PromotionPlaybook playbook) {
        PromotionCampaign campaign = new PromotionCampaign();
        campaign.setPublicId(playbook.getCampaignPublicId());
        campaign.setApprovalStatus(CampaignApprovalStatus.APPROVED);
        Promotion promotion = new Promotion();
        promotion.setPublicId(playbook.getPromotionPublicId());
        promotion.setCampaignPublicId(playbook.getCampaignPublicId());
        promotion.setPromotionType(PromotionType.VOUCHER);
        promotion.setConditionsJson("{}");
        promotion.setActionsJson("{\"discountType\":\"FIXED_AMOUNT\",\"discountValue\":50000}");
        promotion.setMetadataJson("{}");
        when(campaigns.findByPublicId(playbook.getCampaignPublicId()))
                .thenReturn(Optional.of(campaign));
        when(promotions.findByPublicIdAndDeletedAtIsNull(playbook.getPromotionPublicId()))
                .thenReturn(Optional.of(promotion));
    }
}
