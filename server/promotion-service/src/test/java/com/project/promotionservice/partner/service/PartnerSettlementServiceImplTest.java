package com.project.promotionservice.partner.service;

import com.project.promotionservice.benefit.repository.CouponRedemptionRepository;
import com.project.promotionservice.benefit.repository.VoucherRedemptionRepository;
import com.project.promotionservice.common.audit.AuditTrailService;
import com.project.promotionservice.integration.outbox.PromotionDomainEventService;
import com.project.promotionservice.partner.dto.request.SettlementUpdateRequest;
import com.project.promotionservice.partner.entity.PartnerSettlement;
import com.project.promotionservice.partner.enums.SettlementStatus;
import com.project.promotionservice.partner.repository.PartnerRepository;
import com.project.promotionservice.partner.repository.PartnerSettlementRepository;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartnerSettlementServiceImplTest {

    @Mock private PartnerSettlementRepository settlements;
    @Mock private PartnerRepository partners;
    @Mock private PromotionCampaignRepository campaigns;
    @Mock private CouponRedemptionRepository couponRedemptions;
    @Mock private VoucherRedemptionRepository voucherRedemptions;
    @Mock private AuditTrailService audit;
    @Mock private PromotionDomainEventService events;

    @Test
    void disputedSettlementCanBeResolvedAndApproved() {
        PartnerSettlement entity = settlement(SettlementStatus.DISPUTED);
        when(settlements.findByPublicIdForUpdate(entity.getPublicId()))
                .thenReturn(Optional.of(entity));
        when(settlements.save(any(PartnerSettlement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        SettlementUpdateRequest request = new SettlementUpdateRequest();
        request.setStatus(SettlementStatus.APPROVED);

        var response = service().update(entity.getPublicId(), request, "finance-user");

        assertThat(response.getStatus()).isEqualTo(SettlementStatus.APPROVED);
        assertThat(response.getApprovedAt()).isNotNull();
    }

    @Test
    void paidSettlementCanBeClosedAsCompleted() {
        PartnerSettlement entity = settlement(SettlementStatus.PAID);
        Instant paidAt = Instant.now().minusSeconds(60);
        entity.setPaidAt(paidAt);
        when(settlements.findByPublicIdForUpdate(entity.getPublicId()))
                .thenReturn(Optional.of(entity));
        when(settlements.save(any(PartnerSettlement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        SettlementUpdateRequest request = new SettlementUpdateRequest();
        request.setStatus(SettlementStatus.COMPLETED);

        var response = service().update(entity.getPublicId(), request, "finance-user");

        assertThat(response.getStatus()).isEqualTo(SettlementStatus.COMPLETED);
        assertThat(response.getPaidAt()).isEqualTo(paidAt);
    }

    private PartnerSettlementServiceImpl service() {
        return new PartnerSettlementServiceImpl(
                settlements, partners, campaigns, couponRedemptions,
                voucherRedemptions, audit, events);
    }

    private PartnerSettlement settlement(SettlementStatus status) {
        PartnerSettlement entity = new PartnerSettlement();
        entity.setStatus(status);
        entity.setPartnerPublicId("partner-1");
        entity.setSettlementCode("SET-1");
        entity.setSettlementPeriodFrom(Instant.now().minusSeconds(3600));
        entity.setSettlementPeriodTo(Instant.now());
        return entity;
    }
}
