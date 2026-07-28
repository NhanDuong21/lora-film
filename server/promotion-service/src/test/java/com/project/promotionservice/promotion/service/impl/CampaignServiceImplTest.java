package com.project.promotionservice.promotion.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.integration.outbox.PromotionOutboxEventRepository;
import com.project.promotionservice.promotion.dto.request.CampaignCreateRequest;
import com.project.promotionservice.promotion.dto.request.CampaignUpdateRequest;
import com.project.promotionservice.promotion.dto.response.CampaignResponse;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.promotion.enums.CampaignType;
import com.project.promotionservice.promotion.enums.FundingSource;
import com.project.promotionservice.promotion.mapper.CampaignMapper;
import com.project.promotionservice.promotion.repository.ApprovalHistoryRepository;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import com.project.promotionservice.promotion.repository.PromotionRuleRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CampaignServiceImplTest {

    @Mock
    private PromotionCampaignRepository campaignRepository;
    @Mock
    private PromotionRuleRepository ruleRepository;
    @Mock
    private ApprovalHistoryRepository approvalHistoryRepository;
    @Mock
    private PromotionOutboxEventRepository outboxEventRepository;
    @Mock
    private CampaignMapper campaignMapper;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private CampaignServiceImpl campaignService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createCampaign_success() {
        CampaignCreateRequest request = new CampaignCreateRequest();
        request.setCode("SUMMER2026");
        request.setName("Summer 2026 Campaign");
        request.setCampaignType(CampaignType.COUPON);
        request.setFundingSource(FundingSource.SYSTEM);
        request.setStartAt(Instant.now().plusSeconds(3600));
        request.setEndAt(Instant.now().plusSeconds(7200));
        request.setBudgetAmount(new BigDecimal("1000000.00"));

        PromotionCampaign campaignEntity = new PromotionCampaign();
        campaignEntity.setCode("SUMMER2026");
        campaignEntity.setName("Summer 2026 Campaign");
        campaignEntity.setPublicId("550e8400-e29b-41d4-a716-446655440000");

        CampaignResponse responseDto = new CampaignResponse();
        responseDto.setCode("SUMMER2026");
        responseDto.setPublicId("550e8400-e29b-41d4-a716-446655440000");

        when(campaignRepository.existsByCodeAndDeletedAtIsNull("SUMMER2026")).thenReturn(false);
        when(campaignMapper.toEntity(any(CampaignCreateRequest.class))).thenReturn(campaignEntity);
        when(campaignRepository.save(any(PromotionCampaign.class))).thenReturn(campaignEntity);
        when(campaignMapper.toResponse(any(PromotionCampaign.class))).thenReturn(responseDto);

        CampaignResponse result = campaignService.createCampaign(request, "1");

        assertNotNull(result);
        assertEquals("SUMMER2026", result.getCode());
        verify(campaignRepository, times(1)).save(any(PromotionCampaign.class));
    }

    @Test
    void createCampaign_invalidDates_throwsException() {
        CampaignCreateRequest request = new CampaignCreateRequest();
        request.setStartAt(Instant.now().plusSeconds(7200));
        request.setEndAt(Instant.now().plusSeconds(3600)); // end before start

        BusinessException exception = assertThrows(BusinessException.class, () ->
                campaignService.createCampaign(request, "1")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("End date must be after start date", exception.getMessage());
    }

    @Test
    void updateCampaign_nonDraft_throwsException() {
        CampaignUpdateRequest request = new CampaignUpdateRequest();
        request.setName("Updated name");
        request.setStartAt(Instant.now().plusSeconds(3600));
        request.setEndAt(Instant.now().plusSeconds(7200));
        request.setBudgetAmount(new BigDecimal("10000.00"));

        PromotionCampaign existingCampaign = new PromotionCampaign();
        existingCampaign.setPublicId("550e8400-e29b-41d4-a716-446655440000");
        existingCampaign.setStatus(CampaignStatus.ACTIVE); // active, not draft

        when(campaignRepository.findByPublicId("550e8400-e29b-41d4-a716-446655440000")).thenReturn(Optional.of(existingCampaign));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                campaignService.updateCampaign("550e8400-e29b-41d4-a716-446655440000", request, "1")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Only DRAFT campaigns can be updated", exception.getMessage());
    }
}
