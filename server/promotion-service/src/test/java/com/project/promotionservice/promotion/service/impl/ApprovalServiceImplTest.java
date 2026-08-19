package com.project.promotionservice.promotion.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.integration.outbox.PromotionOutboxEventRepository;
import com.project.promotionservice.integration.outbox.PromotionOutboxEnvelopeFactory;
import com.project.promotionservice.promotion.dto.response.CampaignResponse;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.enums.CampaignApprovalStatus;
import com.project.promotionservice.promotion.mapper.ApprovalMapper;
import com.project.promotionservice.promotion.mapper.CampaignMapper;
import com.project.promotionservice.promotion.repository.ApprovalHistoryRepository;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ApprovalServiceImplTest {

    @Mock
    private PromotionCampaignRepository campaignRepository;
    @Mock
    private ApprovalHistoryRepository approvalHistoryRepository;
    @Mock
    private PromotionOutboxEventRepository outboxEventRepository;
    @Mock
    private CampaignMapper campaignMapper;
    @Mock
    private ApprovalMapper approvalMapper;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private PromotionOutboxEnvelopeFactory envelopeFactory;

    @InjectMocks
    private ApprovalServiceImpl approvalService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void approveCampaign_fourEyesViolation_throwsException() {
        PromotionCampaign campaign = new PromotionCampaign();
        campaign.setPublicId("550e8400-e29b-41d4-a716-446655440000");
        campaign.setCreatedBy("user123");
        campaign.setApprovalStatus(CampaignApprovalStatus.PENDING);
        campaign.setBudgetAmount(new BigDecimal("1000000.00"));

        when(campaignRepository.findByPublicId("550e8400-e29b-41d4-a716-446655440000")).thenReturn(Optional.of(campaign));

        // Approver is also creator ("user123")
        BusinessException exception = assertThrows(BusinessException.class, () ->
                approvalService.approveCampaign("550e8400-e29b-41d4-a716-446655440000", "Approved", "user123", List.of("PROMOTION_APPROVE_STANDARD"))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Creator cannot approve their own campaign", exception.getMessage());
    }

    @Test
    void approveCampaign_largeBudgetNoFinanceDirector_throwsException() {
        PromotionCampaign campaign = new PromotionCampaign();
        campaign.setPublicId("550e8400-e29b-41d4-a716-446655440000");
        campaign.setCreatedBy("creator1");
        campaign.setApprovalStatus(CampaignApprovalStatus.PENDING);
        // Budget = 60,000,000 (which exceeds the 50M threshold)
        campaign.setBudgetAmount(new BigDecimal("60000000.00"));

        when(campaignRepository.findByPublicId("550e8400-e29b-41d4-a716-446655440000")).thenReturn(Optional.of(campaign));

        // Standard approval does not grant high-budget authority.
        BusinessException exception = assertThrows(BusinessException.class, () ->
                approvalService.approveCampaign("550e8400-e29b-41d4-a716-446655440000", "Approved", "approver1", List.of("PROMOTION_APPROVE_STANDARD"))
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("Approver lacks capability PROMOTION_APPROVE_HIGH_BUDGET", exception.getMessage());
    }

    @Test
    void approveCampaign_largeBudgetWithFinanceDirector_success() {
        PromotionCampaign campaign = new PromotionCampaign();
        campaign.setPublicId("550e8400-e29b-41d4-a716-446655440000");
        campaign.setCreatedBy("creator1");
        campaign.setApprovalStatus(CampaignApprovalStatus.PENDING);
        campaign.setBudgetAmount(new BigDecimal("60000000.00"));

        CampaignResponse response = new CampaignResponse();
        response.setApprovalStatus(CampaignApprovalStatus.APPROVED);

        when(campaignRepository.findByPublicId("550e8400-e29b-41d4-a716-446655440000")).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(PromotionCampaign.class))).thenReturn(campaign);
        when(campaignMapper.toResponse(any(PromotionCampaign.class))).thenReturn(response);

        CampaignResponse result = approvalService.approveCampaign("550e8400-e29b-41d4-a716-446655440000", "Approved", "approver1", List.of("PROMOTION_APPROVE_HIGH_BUDGET"));

        assertNotNull(result);
        assertEquals(CampaignApprovalStatus.APPROVED, result.getApprovalStatus());
        verify(campaignRepository, times(1)).save(any(PromotionCampaign.class));
        verify(approvalHistoryRepository, times(1)).save(any());
    }

    @ParameterizedTest
    @CsvSource({
            "49999999.00,PROMOTION_APPROVE_STANDARD,PROMOTION_APPROVE_HIGH_BUDGET",
            "50000000.00,PROMOTION_APPROVE_STANDARD,PROMOTION_APPROVE_HIGH_BUDGET",
            "50000001.00,PROMOTION_APPROVE_HIGH_BUDGET,PROMOTION_APPROVE_STANDARD"
    })
    void approvalThresholdBoundaryUsesExpectedCapability(
            String budget, String requiredCapability, String providedCapability) {
        PromotionCampaign campaign = new PromotionCampaign();
        campaign.setPublicId("550e8400-e29b-41d4-a716-446655440009");
        campaign.setCreatedBy("creator1");
        campaign.setApprovalStatus(CampaignApprovalStatus.PENDING);
        campaign.setBudgetAmount(new BigDecimal(budget));
        when(campaignRepository.findByPublicId(campaign.getPublicId()))
                .thenReturn(Optional.of(campaign));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                approvalService.approveCampaign(campaign.getPublicId(),
                        "Boundary check", "approver1", List.of(providedCapability)));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("Approver lacks capability " + requiredCapability,
                exception.getMessage());
    }
}
