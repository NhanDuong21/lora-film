package com.project.promotionservice.integration.job;

import com.project.promotionservice.automation.service.PromotionAutomationBudgetService;
import com.project.promotionservice.common.time.DatabaseTimeProvider;
import com.project.promotionservice.integration.outbox.PromotionDomainEventService;
import com.project.promotionservice.promotion.entity.Promotion;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.entity.UserPromotion;
import com.project.promotionservice.promotion.enums.CampaignApprovalStatus;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.promotion.enums.LegalStatus;
import com.project.promotionservice.promotion.enums.PromotionStatus;
import com.project.promotionservice.promotion.enums.UserPromotionStatus;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import com.project.promotionservice.promotion.repository.PromotionRepository;
import com.project.promotionservice.promotion.repository.UserPromotionRepository;
import com.project.promotionservice.promotion.service.PromotionCatalogEventService;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.EnumSet;

@Service
public class PromotionLifecycleService {

    private final PromotionCampaignRepository campaigns;
    private final PromotionRepository promotions;
    private final UserPromotionRepository wallets;
    private final PromotionDomainEventService events;
    private final DatabaseTimeProvider time;
    private final CacheManager cacheManager;
    private final PromotionAutomationBudgetService automationBudgetService;

    public PromotionLifecycleService(
            PromotionCampaignRepository campaigns,
            PromotionRepository promotions,
            UserPromotionRepository wallets,
            PromotionDomainEventService events,
            DatabaseTimeProvider time,
            CacheManager cacheManager,
            PromotionAutomationBudgetService automationBudgetService) {
        this.campaigns = campaigns;
        this.promotions = promotions;
        this.wallets = wallets;
        this.events = events;
        this.time = time;
        this.cacheManager = cacheManager;
        this.automationBudgetService = automationBudgetService;
    }

    @Transactional
    public int activateCampaigns(String actor) {
        Instant now = time.now();
        var ids = campaigns.findActivatableIds(
                CampaignStatus.SCHEDULED, now, PageRequest.of(0, 200));
        int count = 0;
        for (String id : ids) {
            PromotionCampaign campaign = campaigns.findByPublicIdForUpdate(id).orElse(null);
            if (!canActivate(campaign, now)) {
                continue;
            }
            campaign.setStatus(CampaignStatus.ACTIVE);
            campaign.setUpdatedBy(actor);
            campaigns.save(campaign);
            promotions.activateDraftPromotions(
                    id, PromotionStatus.DRAFT, PromotionStatus.ACTIVE, now, actor);
            events.enqueue("CAMPAIGN", id, "CAMPAIGN_AUTO_ACTIVATED",
                    "promotion.campaign.lifecycle", campaign, actor);
            count++;
        }
        invalidateCachesAfterCommit(count > 0);
        return count;
    }

    @Transactional
    public int activatePromotions(String actor) {
        Instant now = time.now();
        var ids = promotions.findActivatableIds(
                PromotionStatus.DRAFT, CampaignStatus.ACTIVE,
                com.project.promotionservice.promotion.enums.LegalStatus.PASSED,
                now, PageRequest.of(0, 500));
        int count = 0;
        for (String id : ids) {
            Promotion promotion = promotions.findByPublicIdForUpdate(id).orElse(null);
            if (promotion == null
                    || promotion.getStatus() != PromotionStatus.DRAFT
                    || promotion.getValidFrom().isAfter(now)
                    || !promotion.getValidTo().isAfter(now)) {
                continue;
            }
            PromotionCampaign campaign = campaigns
                    .findByPublicIdAndDeletedAtIsNull(
                            promotion.getCampaignPublicId()).orElse(null);
            if (!runtimeCampaign(campaign, now)) {
                continue;
            }
            promotion.setStatus(PromotionStatus.ACTIVE);
            promotion.setUpdatedBy(actor);
            promotions.save(promotion);
            events.enqueue("PROMOTION", id, "PROMOTION_ACTIVATED",
                    PromotionCatalogEventService.PROMOTION_LIFECYCLE_TOPIC, promotion, actor);
            count++;
        }
        invalidateCachesAfterCommit(count > 0);
        return count;
    }

    @Transactional
    public int expireCampaigns(String actor) {
        Instant now = time.now();
        var ids = campaigns.findExpiredIds(
                EnumSet.of(CampaignStatus.SCHEDULED, CampaignStatus.ACTIVE,
                        CampaignStatus.PAUSED),
                now, PageRequest.of(0, 200));
        int count = 0;
        for (String id : ids) {
            PromotionCampaign campaign = campaigns.findByPublicIdForUpdate(id).orElse(null);
            if (campaign == null || campaign.getEndAt().isAfter(now)
                    || campaign.getStatus() == CampaignStatus.COMPLETED
                    || campaign.getStatus() == CampaignStatus.CANCELLED
                    || campaign.getStatus() == CampaignStatus.KILLED) {
                continue;
            }
            campaign.setStatus(CampaignStatus.COMPLETED);
            campaign.setUpdatedBy(actor);
            campaigns.save(campaign);
            events.enqueue("CAMPAIGN", id, "CAMPAIGN_EXPIRED",
                    "promotion.campaign.lifecycle", campaign, actor);
            count++;
        }
        invalidateCachesAfterCommit(count > 0);
        return count;
    }

    @Transactional
    public int expirePromotions(String actor) {
        Instant now = time.now();
        var ids = promotions.findExpirableIds(
                EnumSet.of(PromotionStatus.ACTIVE, PromotionStatus.PAUSED),
                now, PageRequest.of(0, 500));
        int count = 0;
        for (String id : ids) {
            Promotion promotion = promotions.findByPublicIdForUpdate(id).orElse(null);
            if (promotion == null || promotion.getValidTo().isAfter(now)
                    || (promotion.getStatus() != PromotionStatus.ACTIVE
                    && promotion.getStatus() != PromotionStatus.PAUSED)) {
                continue;
            }
            promotion.setStatus(PromotionStatus.EXPIRED);
            promotion.setUpdatedBy(actor);
            promotions.save(promotion);
            events.enqueue("PROMOTION", id, "PROMOTION_EXPIRED",
                    PromotionCatalogEventService.PROMOTION_LIFECYCLE_TOPIC, promotion, actor);
            count++;
        }
        invalidateCachesAfterCommit(count > 0);
        return count;
    }

    @Transactional
    public int expireWalletItems(String actor) {
        Instant now = time.now();
        var ids = wallets.findExpirableIds(
                EnumSet.of(UserPromotionStatus.AVAILABLE),
                now, PageRequest.of(0, 500));
        int count = 0;
        for (String id : ids) {
            UserPromotion wallet = wallets.findByPublicIdForUpdate(id).orElse(null);
            if (wallet == null || wallet.getValidTo().isAfter(now)
                    || wallet.getStatus() != UserPromotionStatus.AVAILABLE) {
                continue;
            }
            wallet.setStatus(UserPromotionStatus.EXPIRED);
            wallet.setUpdatedBy(actor);
            wallets.save(wallet);
            automationBudgetService.releaseForWallet(
                    wallet.getAudienceMemberPublicId(), wallet.getAutomationRunPublicId());
            count++;
        }
        invalidateCachesAfterCommit(count > 0);
        return count;
    }

    private boolean canActivate(PromotionCampaign campaign, Instant now) {
        return campaign != null
                && campaign.getStatus() == CampaignStatus.SCHEDULED
                && Boolean.TRUE.equals(campaign.getAutoActivate())
                && !Boolean.TRUE.equals(campaign.getKillSwitch())
                && campaign.getApprovalStatus() == CampaignApprovalStatus.APPROVED
                && campaign.getLegalStatus() == LegalStatus.PASSED
                && campaign.getBudgetAmount() != null
                && campaign.getBudgetAmount().signum() > 0
                && !campaign.getStartAt().isAfter(now)
                && campaign.getEndAt().isAfter(now);
    }

    private boolean runtimeCampaign(PromotionCampaign campaign, Instant now) {
        return campaign != null
                && campaign.getStatus() == CampaignStatus.ACTIVE
                && campaign.getApprovalStatus() == CampaignApprovalStatus.APPROVED
                && campaign.getLegalStatus() == LegalStatus.PASSED
                && !Boolean.TRUE.equals(campaign.getKillSwitch())
                && campaign.getEndAt().isAfter(now);
    }

    private void invalidateCachesAfterCommit(boolean changed) {
        if (!changed) {
            return;
        }
        Runnable clear = () -> {
            for (String name : new String[]{"promotions", "campaigns", "reservations"}) {
                var cache = cacheManager.getCache(name);
                if (cache != null) {
                    cache.clear();
                }
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            clear.run();
                        }
                    });
        } else {
            clear.run();
        }
    }
}
