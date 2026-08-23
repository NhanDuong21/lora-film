package com.project.promotionservice.promotion.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.automation.repository.PromotionAutomationRunRepository;
import com.project.promotionservice.automation.repository.PromotionPlaybookRepository;
import com.project.promotionservice.automation.service.PromotionAnomalyCaseService;
import com.project.promotionservice.common.audit.AuditTrailService;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.configuration.security.principal.UserPrincipal;
import com.project.promotionservice.promotion.dto.response.PromotionIssueResponse;
import com.project.promotionservice.promotion.entity.Promotion;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.enums.CampaignScopeType;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.promotion.enums.PromotionDistributionMode;
import com.project.promotionservice.promotion.enums.PromotionStatus;
import com.project.promotionservice.promotion.enums.PromotionType;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import com.project.promotionservice.promotion.repository.PromotionRepository;
import com.project.promotionservice.promotion.service.PromotionCatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManagerPromotionWorkspaceServiceTest {
    @Mock private PromotionCampaignRepository campaignRepository;
    @Mock private PromotionRepository promotionRepository;
    @Mock private PromotionPlaybookRepository playbookRepository;
    @Mock private PromotionAutomationRunRepository runRepository;
    @Mock private PromotionAnomalyCaseService anomalyCaseService;
    @Mock private PromotionCatalogService catalogService;
    @Mock private AuditTrailService auditTrailService;

    private ManagerPromotionWorkspaceService service;

    @BeforeEach
    void setUp() {
        service = new ManagerPromotionWorkspaceService(campaignRepository,
                promotionRepository, playbookRepository, runRepository,
                anomalyCaseService, catalogService, auditTrailService,
                new ObjectMapper());
    }

    @Test
    void localIssuanceRevalidatesCinemaAndAuditsManagerAndCinema() {
        PromotionCampaign campaign = campaign("cinema-a");
        Promotion promotion = promotion(campaign, "cinema-a",
                PromotionDistributionMode.ASSIGNED_WALLET);
        when(promotionRepository.findByPublicIdAndDeletedAtIsNull(
                promotion.getPublicId())).thenReturn(Optional.of(promotion));
        when(campaignRepository.findByPublicIdAndDeletedAtIsNull(
                campaign.getPublicId())).thenReturn(Optional.of(campaign));
        when(catalogService.issue(promotion.getPublicId(), List.of("customer-1"), "42"))
                .thenReturn(new PromotionIssueResponse(1, 0, List.of()));

        PromotionIssueResponse response = service.issue("cinema-a",
                promotion.getPublicId(), List.of("customer-1"), principal(
                        Set.of("cinema-a"), List.of("PROMOTION_VIEW",
                                "PROMOTION_DISTRIBUTE_LOCAL")));

        assertThat(response.issuedCount()).isEqualTo(1);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> audit = ArgumentCaptor.forClass(Map.class);
        verify(auditTrailService).record(eq("MANAGER_PROMOTION_DISTRIBUTION"),
                eq(promotion.getPublicId()), eq("LOCAL_BENEFIT_ISSUED"),
                eq(null), audit.capture(), eq("42"));
        assertThat(audit.getValue()).containsEntry("managerAccountId", "42")
                .containsEntry("cinemaPublicId", "cinema-a")
                .containsEntry("issuedCount", 1);
    }

    @Test
    void localIssuanceCannotUseAClientSelectedCinemaOutsideAssignment() {
        assertThatThrownBy(() -> service.issue("cinema-b", "promotion-1",
                List.of("customer-1"), principal(Set.of("cinema-a"),
                        List.of("PROMOTION_VIEW", "PROMOTION_DISTRIBUTE_LOCAL"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("outside the manager's assigned scope");

        verify(catalogService, never()).issue(any(), any(), any());
    }

    @Test
    void malformedPromotionScopeFailsClosed() {
        PromotionCampaign campaign = campaign("cinema-a");
        Promotion promotion = promotion(campaign, "cinema-a",
                PromotionDistributionMode.ASSIGNED_WALLET);
        promotion.setConditionsJson("not-json");
        when(campaignRepository.findAll()).thenReturn(List.of(campaign));
        when(promotionRepository.findAll()).thenReturn(List.of(promotion));

        assertThat(service.distributionOptions("cinema-a", principal(
                Set.of("cinema-a"), List.of("PROMOTION_VIEW")))).isEmpty();
    }

    private UserPrincipal principal(Set<String> cinemas, List<String> permissions) {
        return new UserPrincipal(42L, "manager", "manager@lorafilm.vn",
                List.of("MANAGER"), permissions, cinemas,
                permissions.stream().map(SimpleGrantedAuthority::new).toList());
    }

    private PromotionCampaign campaign(String cinemaId) {
        PromotionCampaign campaign = new PromotionCampaign();
        campaign.setCode("LOCAL");
        campaign.setName("Local campaign");
        campaign.setScopeType(CampaignScopeType.ASSIGNED_CINEMAS);
        campaign.setCinemaScopeJson("[\"" + cinemaId + "\"]");
        campaign.setStatus(CampaignStatus.ACTIVE);
        campaign.setStartAt(Instant.now().minusSeconds(3600));
        campaign.setEndAt(Instant.now().plusSeconds(3600));
        return campaign;
    }

    private Promotion promotion(
            PromotionCampaign campaign, String cinemaId,
            PromotionDistributionMode mode) {
        Promotion promotion = new Promotion();
        promotion.setCampaignPublicId(campaign.getPublicId());
        promotion.setPromotionType(PromotionType.VOUCHER);
        promotion.setName("Local benefit");
        promotion.setStatus(PromotionStatus.ACTIVE);
        promotion.setDistributionMode(mode);
        promotion.setConditionsJson("{\"cinemaPublicIds\":[\"" + cinemaId + "\"]}");
        promotion.setActionsJson("{}");
        promotion.setValidFrom(Instant.now().minusSeconds(3600));
        promotion.setValidTo(Instant.now().plusSeconds(3600));
        return promotion;
    }
}
