package com.project.promotionservice.integration.job;

import com.project.promotionservice.benefit.entity.Coupon;
import com.project.promotionservice.benefit.entity.Voucher;
import com.project.promotionservice.benefit.enums.BenefitEnums.CouponStatus;
import com.project.promotionservice.benefit.enums.BenefitEnums.VoucherStatus;
import com.project.promotionservice.benefit.repository.CouponRepository;
import com.project.promotionservice.benefit.repository.VoucherRepository;
import com.project.promotionservice.common.time.DatabaseTimeProvider;
import com.project.promotionservice.integration.outbox.PromotionDomainEventService;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.promotion.enums.CampaignApprovalStatus;
import com.project.promotionservice.promotion.enums.LegalStatus;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.EnumSet;

@Service
public class PromotionLifecycleService {
    private final PromotionCampaignRepository campaigns;
    private final CouponRepository coupons;
    private final VoucherRepository vouchers;
    private final PromotionDomainEventService events;
    private final DatabaseTimeProvider time;
    private final CacheManager cacheManager;

    public PromotionLifecycleService(PromotionCampaignRepository campaigns,
                                     CouponRepository coupons,
                                     VoucherRepository vouchers,
                                     PromotionDomainEventService events,
                                     DatabaseTimeProvider time,
                                     CacheManager cacheManager) {
        this.campaigns = campaigns; this.coupons = coupons; this.vouchers = vouchers;
        this.events = events; this.time = time; this.cacheManager = cacheManager;
    }

    @Transactional
    public int activateCampaigns(String actor) {
        Instant now = time.now();
        var ids = campaigns.findActivatableIds(
                CampaignStatus.SCHEDULED, now, PageRequest.of(0, 200));
        int count = 0;
        for (String id : ids) {
            PromotionCampaign campaign = campaigns.findByPublicIdForUpdate(id).orElse(null);
            if (campaign == null
                    || campaign.getStatus() != CampaignStatus.SCHEDULED
                    || !Boolean.TRUE.equals(campaign.getAutoActivate())
                    || Boolean.TRUE.equals(campaign.getKillSwitch())
                    || campaign.getApprovalStatus() != CampaignApprovalStatus.APPROVED
                    || campaign.getLegalStatus() != LegalStatus.PASSED
                    || campaign.getBudgetAmount() == null
                    || campaign.getBudgetAmount().signum() <= 0
                    || campaign.getStartAt() == null
                    || campaign.getStartAt().isAfter(now)
                    || campaign.getEndAt() == null
                    || !campaign.getEndAt().isAfter(now)) {
                continue;
            }
            campaign.setStatus(CampaignStatus.ACTIVE);
            campaign.setUpdatedBy(actor);
            campaigns.save(campaign);
            events.enqueue("CAMPAIGN", id, "CAMPAIGN_AUTO_ACTIVATED",
                    "promotion.campaign.lifecycle", campaign, actor);
            count++;
        }
        invalidateCachesAfterCommit(count > 0);
        return count;
    }

    @Transactional
    public int expireCampaigns(String actor) {
        Instant now = time.now();
        var ids = campaigns.findExpiredIds(
                EnumSet.of(CampaignStatus.SCHEDULED, CampaignStatus.ACTIVE, CampaignStatus.PAUSED),
                now, PageRequest.of(0, 200));
        int count = 0;
        for (String id : ids) {
            PromotionCampaign campaign = campaigns.findByPublicIdForUpdate(id).orElse(null);
            if (campaign == null || campaign.getEndAt() == null || campaign.getEndAt().isAfter(now)
                    || campaign.getStatus() == CampaignStatus.COMPLETED
                    || campaign.getStatus() == CampaignStatus.CANCELLED) continue;
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
    public int expireCoupons(String actor) {
        Instant now = time.now();
        var ids = coupons.findExpirableIds(
                EnumSet.of(CouponStatus.ACTIVE, CouponStatus.LOCKED), now, PageRequest.of(0, 500));
        int count = 0;
        for (String id : ids) {
            Coupon coupon = coupons.findByPublicIdForUpdate(id).orElse(null);
            if (coupon == null || coupon.getValidTo().isAfter(now)
                    || (coupon.getStatus() != CouponStatus.ACTIVE && coupon.getStatus() != CouponStatus.LOCKED)) continue;
            coupon.setStatus(CouponStatus.EXPIRED); coupon.setUpdatedBy(actor); coupons.save(coupon);
            events.enqueue("COUPON", id, "COUPON_EXPIRED", "promotion.benefit.lifecycle", coupon, actor);
            count++;
        }
        invalidateCachesAfterCommit(count > 0);
        return count;
    }

    @Transactional
    public int expireVouchers(String actor) {
        Instant now = time.now();
        var ids = vouchers.findExpirableIds(
                EnumSet.of(VoucherStatus.ISSUED, VoucherStatus.ACTIVE, VoucherStatus.LOCKED),
                now, PageRequest.of(0, 500));
        int count = 0;
        for (String id : ids) {
            Voucher voucher = vouchers.findByPublicIdForUpdate(id).orElse(null);
            if (voucher == null || voucher.getValidTo().isAfter(now)
                    || (voucher.getStatus() != VoucherStatus.ISSUED
                    && voucher.getStatus() != VoucherStatus.ACTIVE
                    && voucher.getStatus() != VoucherStatus.LOCKED)) continue;
            voucher.setStatus(VoucherStatus.EXPIRED); voucher.setUpdatedBy(actor); vouchers.save(voucher);
            events.enqueue("VOUCHER", id, "VOUCHER_EXPIRED", "promotion.benefit.lifecycle", voucher, actor);
            count++;
        }
        invalidateCachesAfterCommit(count > 0);
        return count;
    }

    private void invalidateCachesAfterCommit(boolean changed) {
        if (!changed) return;
        Runnable clear = () -> {
            for (String name : new String[]{"promotions", "campaigns", "vouchers", "reservations"}) {
                var cache = cacheManager.getCache(name);
                if (cache != null) cache.clear();
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
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
