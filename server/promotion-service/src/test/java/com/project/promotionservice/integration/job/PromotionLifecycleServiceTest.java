package com.project.promotionservice.integration.job;

import com.project.promotionservice.benefit.repository.CouponRepository;
import com.project.promotionservice.benefit.repository.VoucherRepository;
import com.project.promotionservice.common.time.DatabaseTimeProvider;
import com.project.promotionservice.integration.outbox.PromotionDomainEventService;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.enums.CampaignApprovalStatus;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.promotion.enums.LegalStatus;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionLifecycleServiceTest {

    @Mock private PromotionCampaignRepository campaigns;
    @Mock private CouponRepository coupons;
    @Mock private VoucherRepository vouchers;
    @Mock private PromotionDomainEventService events;
    @Mock private DatabaseTimeProvider time;
    @Mock private CacheManager cacheManager;

    @Test
    void activatesOnlyAnApprovedLegallyPassedScheduledCampaign() {
        Instant now = Instant.parse("2026-07-29T08:00:00Z");
        PromotionCampaign campaign = scheduledCampaign(now);
        when(time.now()).thenReturn(now);
        when(campaigns.findActivatableIds(
                eq(CampaignStatus.SCHEDULED), eq(now), any(Pageable.class)))
                .thenReturn(List.of(campaign.getPublicId()));
        when(campaigns.findByPublicIdForUpdate(campaign.getPublicId()))
                .thenReturn(Optional.of(campaign));
        when(campaigns.save(campaign)).thenReturn(campaign);

        int activated = service().activateCampaigns("PROMOTION_SCHEDULER");

        assertThat(activated).isEqualTo(1);
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.ACTIVE);
        verify(events).enqueue(
                eq("CAMPAIGN"), eq(campaign.getPublicId()),
                eq("CAMPAIGN_AUTO_ACTIVATED"), eq("promotion.campaign.lifecycle"),
                eq(campaign), eq("PROMOTION_SCHEDULER"));
    }

    @Test
    void skipsAStartedCampaignThatHasNotPassedLegalReview() {
        Instant now = Instant.parse("2026-07-29T08:00:00Z");
        PromotionCampaign campaign = scheduledCampaign(now);
        campaign.setLegalStatus(LegalStatus.PENDING);
        when(time.now()).thenReturn(now);
        when(campaigns.findActivatableIds(
                eq(CampaignStatus.SCHEDULED), eq(now), any(Pageable.class)))
                .thenReturn(List.of(campaign.getPublicId()));
        when(campaigns.findByPublicIdForUpdate(campaign.getPublicId()))
                .thenReturn(Optional.of(campaign));

        int activated = service().activateCampaigns("PROMOTION_SCHEDULER");

        assertThat(activated).isZero();
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.SCHEDULED);
        verify(campaigns, never()).save(any());
        verify(events, never()).enqueue(any(), any(), any(), any(), any(), any());
    }

    private PromotionLifecycleService service() {
        return new PromotionLifecycleService(
                campaigns, coupons, vouchers, events, time, cacheManager);
    }

    private PromotionCampaign scheduledCampaign(Instant now) {
        PromotionCampaign campaign = new PromotionCampaign();
        campaign.setStatus(CampaignStatus.SCHEDULED);
        campaign.setAutoActivate(true);
        campaign.setKillSwitch(false);
        campaign.setApprovalStatus(CampaignApprovalStatus.APPROVED);
        campaign.setLegalStatus(LegalStatus.PASSED);
        campaign.setBudgetAmount(new BigDecimal("1000000.00"));
        campaign.setStartAt(now.minusSeconds(60));
        campaign.setEndAt(now.plusSeconds(3600));
        return campaign;
    }
}
