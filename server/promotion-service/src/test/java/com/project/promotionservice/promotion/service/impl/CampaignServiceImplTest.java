package com.project.promotionservice.promotion.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.integration.outbox.PromotionOutboxEventRepository;
import com.project.promotionservice.integration.outbox.PromotionOutboxEnvelopeFactory;
import com.project.promotionservice.promotion.dto.request.CampaignCreateRequest;
import com.project.promotionservice.promotion.dto.request.CampaignUpdateRequest;
import com.project.promotionservice.promotion.dto.request.LegalReviewRequest;
import com.project.promotionservice.promotion.dto.response.CampaignResponse;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.promotion.enums.CampaignApprovalStatus;
import com.project.promotionservice.promotion.enums.LegalStatus;
import com.project.promotionservice.reservation.enums.ReservationStatus;
import com.project.promotionservice.promotion.mapper.CampaignMapper;
import com.project.promotionservice.promotion.repository.ApprovalHistoryRepository;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import com.project.promotionservice.promotion.repository.PromotionRepository;
import com.project.promotionservice.promotion.repository.PromotionRedemptionRepository;
import com.project.promotionservice.promotion.repository.UserPromotionRepository;
import com.project.promotionservice.promotion.service.CampaignConfigurationPolicy;
import com.project.promotionservice.reservation.repository.PromotionReservationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
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
    private PromotionRepository promotionRepository;
    @Mock
    private ApprovalHistoryRepository approvalHistoryRepository;
    @Mock
    private PromotionOutboxEventRepository outboxEventRepository;
    @Mock
    private CampaignMapper campaignMapper;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private PromotionReservationRepository reservationRepository;
    @Mock
    private PromotionRedemptionRepository redemptionRepository;
    @Mock
    private UserPromotionRepository walletRepository;
    @Mock
    private PromotionOutboxEnvelopeFactory envelopeFactory;
    @Spy
    private CampaignConfigurationPolicy configurationPolicy =
            new CampaignConfigurationPolicy();

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

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals(
                "Campaign configuration is locked after submission; reject it before editing",
                exception.getMessage());
    }

    @Test
    void publishCampaign_requiresPassedLegalReview() {
        PromotionCampaign campaign = new PromotionCampaign();
        campaign.setPublicId("550e8400-e29b-41d4-a716-446655440001");
        campaign.setStatus(CampaignStatus.DRAFT);
        campaign.setApprovalStatus(CampaignApprovalStatus.APPROVED);
        campaign.setLegalStatus(LegalStatus.PENDING);
        campaign.setBudgetAmount(new BigDecimal("100000.00"));
        when(campaignRepository.findByPublicId(campaign.getPublicId()))
                .thenReturn(Optional.of(campaign));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> campaignService.publishCampaign(campaign.getPublicId(), "admin"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("legal compliance"));
        verify(campaignRepository, never()).save(campaign);
    }

    @Test
    void submitCampaign_requiresAConfiguredRuntimeBenefit() {
        PromotionCampaign campaign = draftCouponCampaign();
        when(campaignRepository.findByPublicId(campaign.getPublicId()))
                .thenReturn(Optional.of(campaign));
        when(promotionRepository.existsConfiguredForCampaign(
                eq(campaign.getPublicId()), any(), eq(campaign.getStartAt()), eq(campaign.getEndAt())))
                .thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> campaignService.submitCampaign(campaign.getPublicId(), "Ready", "marketing"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertTrue(exception.getMessage().contains("configured promotion"));
    }

    @Test
    void submitCampaign_withConfiguredCoupon_locksConfigurationForReview() {
        PromotionCampaign campaign = draftCouponCampaign();
        CampaignResponse response = new CampaignResponse();
        response.setApprovalStatus(CampaignApprovalStatus.PENDING);
        when(campaignRepository.findByPublicId(campaign.getPublicId()))
                .thenReturn(Optional.of(campaign));
        when(promotionRepository.existsConfiguredForCampaign(
                eq(campaign.getPublicId()), any(), eq(campaign.getStartAt()), eq(campaign.getEndAt())))
                .thenReturn(true);
        when(campaignRepository.save(campaign)).thenReturn(campaign);
        when(campaignMapper.toResponse(campaign)).thenReturn(response);

        CampaignResponse result = campaignService.submitCampaign(
                campaign.getPublicId(), "Ready", "marketing");

        assertEquals(CampaignApprovalStatus.PENDING, result.getApprovalStatus());
        assertEquals(CampaignApprovalStatus.PENDING, campaign.getApprovalStatus());
        verify(approvalHistoryRepository).save(any());
    }

    @Test
    void legalReview_beforeSubmission_isRejected() {
        PromotionCampaign campaign = draftCouponCampaign();
        LegalReviewRequest request = new LegalReviewRequest();
        request.setStatus(LegalStatus.PASSED);
        request.setComment("Compliant");
        when(campaignRepository.findByPublicId(campaign.getPublicId()))
                .thenReturn(Optional.of(campaign));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> campaignService.reviewLegalStatus(
                        campaign.getPublicId(), request, "legal"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertTrue(exception.getMessage().contains("after the campaign is submitted"));
        verify(campaignRepository, never()).save(campaign);
    }

    @Test
    void killSwitch_cannotMoveDraftCampaignToPaused() {
        PromotionCampaign campaign = draftCouponCampaign();
        when(campaignRepository.findByPublicIdForUpdate(campaign.getPublicId()))
                .thenReturn(Optional.of(campaign));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> campaignService.killSwitchCampaign(
                        campaign.getPublicId(), "Emergency", "admin"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("SCHEDULED, ACTIVE or PAUSED"));
        assertEquals(CampaignStatus.DRAFT, campaign.getStatus());
        verify(campaignRepository, never()).save(campaign);
    }

    @Test
    void deleteCampaign_rejectsAnActiveReservation() {
        PromotionCampaign campaign = new PromotionCampaign();
        campaign.setPublicId("550e8400-e29b-41d4-a716-446655440002");
        campaign.setStatus(CampaignStatus.DRAFT);
        when(campaignRepository.findByPublicId(campaign.getPublicId()))
                .thenReturn(Optional.of(campaign));
        when(reservationRepository.countByCampaignAndStatus(
                campaign.getPublicId(), ReservationStatus.ACTIVE))
                .thenReturn(1L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> campaignService.deleteCampaign(campaign.getPublicId(), "admin"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(campaignRepository, never()).save(campaign);
    }

    private PromotionCampaign draftCouponCampaign() {
        PromotionCampaign campaign = new PromotionCampaign();
        campaign.setPublicId("550e8400-e29b-41d4-a716-446655440099");
        campaign.setStatus(CampaignStatus.DRAFT);
        campaign.setApprovalStatus(CampaignApprovalStatus.DRAFT);
        campaign.setLegalStatus(LegalStatus.PENDING);
        campaign.setStartAt(Instant.now().plusSeconds(3600));
        campaign.setEndAt(Instant.now().plusSeconds(7200));
        campaign.setBudgetAmount(new BigDecimal("100000.00"));
        return campaign;
    }
}
