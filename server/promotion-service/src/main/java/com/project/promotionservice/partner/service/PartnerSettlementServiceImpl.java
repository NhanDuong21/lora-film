package com.project.promotionservice.partner.service;

import com.project.promotionservice.benefit.enums.BenefitEnums.RedemptionStatus;
import com.project.promotionservice.benefit.repository.CouponRedemptionRepository;
import com.project.promotionservice.benefit.repository.VoucherRedemptionRepository;
import com.project.promotionservice.common.audit.AuditTrailService;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.common.exception.ErrorCode;
import com.project.promotionservice.common.response.PagedResponse;
import com.project.promotionservice.integration.outbox.PromotionDomainEventService;
import com.project.promotionservice.partner.dto.request.SettlementCreateRequest;
import com.project.promotionservice.partner.dto.request.SettlementUpdateRequest;
import com.project.promotionservice.partner.dto.response.SettlementResponse;
import com.project.promotionservice.partner.entity.PartnerSettlement;
import com.project.promotionservice.partner.enums.SettlementRule;
import com.project.promotionservice.partner.enums.SettlementStatus;
import com.project.promotionservice.partner.enums.PartnerStatus;
import com.project.promotionservice.partner.repository.PartnerRepository;
import com.project.promotionservice.partner.repository.PartnerSettlementRepository;
import com.project.promotionservice.partner.repository.PartnerSpecifications;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.EnumSet;

@Service
public class PartnerSettlementServiceImpl implements PartnerSettlementService {
    private static final String TOPIC = "promotion.partner.settlement";
    private static final EnumSet<RedemptionStatus> CONFIRMED =
            EnumSet.of(RedemptionStatus.CONFIRMED, RedemptionStatus.SUCCESS);
    private final PartnerSettlementRepository repository;
    private final PartnerRepository partnerRepository;
    private final PromotionCampaignRepository campaignRepository;
    private final CouponRedemptionRepository couponRedemptions;
    private final VoucherRedemptionRepository voucherRedemptions;
    private final AuditTrailService audit;
    private final PromotionDomainEventService events;

    public PartnerSettlementServiceImpl(PartnerSettlementRepository repository,
                                         PartnerRepository partnerRepository,
                                         PromotionCampaignRepository campaignRepository,
                                         CouponRedemptionRepository couponRedemptions,
                                         VoucherRedemptionRepository voucherRedemptions,
                                         AuditTrailService audit,
                                         PromotionDomainEventService events) {
        this.repository = repository;
        this.partnerRepository = partnerRepository;
        this.campaignRepository = campaignRepository;
        this.couponRedemptions = couponRedemptions;
        this.voucherRedemptions = voucherRedemptions;
        this.audit = audit;
        this.events = events;
    }

    @Override
    @Transactional
    public SettlementResponse create(SettlementCreateRequest request, String actor) {
        requirePartner(request.getPartnerPublicId());
        if (request.getCampaignPublicId() != null) {
            var campaign = campaignRepository.findByPublicId(request.getCampaignPublicId())
                    .orElseThrow(() -> notFound("Campaign not found"));
            if (!request.getPartnerPublicId().equals(campaign.getPartnerPublicId())) {
                throw bad("Campaign is not mapped to the selected partner");
            }
        }
        if (!request.getSettlementPeriodTo().isAfter(request.getSettlementPeriodFrom())) {
            throw bad("Settlement period end must be after start");
        }
        validateRule(request.getSettlementRule(), request.getPartnerPercentage(),
                request.getFixedAmountPerRedemption());
        if (repository.existsByPartnerPublicIdAndSettlementPeriodFromLessThanAndSettlementPeriodToGreaterThanAndDeletedAtIsNull(
                request.getPartnerPublicId(), request.getSettlementPeriodTo(),
                request.getSettlementPeriodFrom())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER,
                    "Overlapping settlement already exists for partner", HttpStatus.CONFLICT);
        }
        Aggregate aggregate = aggregate(request.getCampaignPublicId(), request.getPartnerPublicId(),
                request.getSettlementPeriodFrom(), request.getSettlementPeriodTo());
        PartnerSettlement entity = new PartnerSettlement();
        entity.setPartnerPublicId(request.getPartnerPublicId());
        entity.setCampaignPublicId(request.getCampaignPublicId());
        entity.setSettlementPeriodFrom(request.getSettlementPeriodFrom());
        entity.setSettlementPeriodTo(request.getSettlementPeriodTo());
        entity.setSettlementCode(nextCode(request.getPartnerPublicId(), request.getSettlementPeriodFrom()));
        entity.setTotalOrders(aggregate.orders());
        entity.setTotalDiscount(aggregate.discount());
        entity.setSettlementRule(request.getSettlementRule());
        entity.setPartnerPercentage(scale(request.getPartnerPercentage()));
        entity.setFixedAmountPerRedemption(request.getFixedAmountPerRedemption());
        entity.setPartnerAmount(calculatePartnerAmount(entity, aggregate));
        if (entity.getPartnerAmount().compareTo(aggregate.discount()) > 0) {
            throw bad("Partner settlement amount cannot exceed confirmed discount");
        }
        entity.setPlatformAmount(aggregate.discount().subtract(entity.getPartnerAmount()));
        entity.setFinalAmount(entity.getPartnerAmount());
        entity.setCurrency(request.getCurrency());
        entity.setStatus(SettlementStatus.PENDING_APPROVAL);
        entity.setNote(request.getNote());
        entity.setCreatedBy(actor);
        entity.setUpdatedBy(actor);
        PartnerSettlement saved = repository.save(entity);
        SettlementResponse response = toResponse(saved);
        audit.record("PARTNER_SETTLEMENT", saved.getPublicId(), "SETTLEMENT_CREATED",
                null, response, actor);
        events.enqueue("PARTNER_SETTLEMENT", saved.getPublicId(), "SETTLEMENT_CREATED",
                TOPIC, response, actor);
        return response;
    }

    @Override
    @Transactional
    public SettlementResponse update(String publicId, SettlementUpdateRequest request, String actor) {
        PartnerSettlement entity = repository.findByPublicIdForUpdate(publicId)
                .orElseThrow(() -> notFound("Settlement not found"));
        if (entity.getStatus() == SettlementStatus.COMPLETED) {
            throw bad("Completed settlement cannot be modified");
        }
        if (entity.getStatus() == SettlementStatus.PAID
                && (request.getAdjustmentAmount() != null
                || request.getStatus() != SettlementStatus.COMPLETED)) {
            throw bad("Paid settlement can only be marked completed");
        }
        SettlementResponse before = toResponse(entity);
        if (request.getAdjustmentAmount() != null) {
            entity.setAdjustmentAmount(scale(request.getAdjustmentAmount()));
            entity.setFinalAmount(entity.getPartnerAmount().add(entity.getAdjustmentAmount()));
        }
        if (request.getNote() != null) entity.setNote(request.getNote());
        if (request.getStatus() != null) transition(entity, request.getStatus());
        entity.setUpdatedBy(actor);
        PartnerSettlement saved = repository.save(entity);
        SettlementResponse response = toResponse(saved);
        audit.record("PARTNER_SETTLEMENT", publicId, "SETTLEMENT_UPDATED", before, response, actor);
        events.enqueue("PARTNER_SETTLEMENT", publicId, "SETTLEMENT_UPDATED", TOPIC, response, actor);
        return response;
    }

    @Override
    @Transactional
    public void disable(String publicId, String actor) {
        PartnerSettlement entity = repository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> notFound("Settlement not found"));
        if (entity.getStatus() == SettlementStatus.PAID || entity.getStatus() == SettlementStatus.COMPLETED) {
            throw bad("Completed settlement cannot be disabled");
        }
        SettlementResponse before = toResponse(entity);
        entity.setStatus(SettlementStatus.CANCELLED);
        entity.setDeletedAt(Instant.now());
        entity.setDeletedBy(actor);
        entity.setUpdatedBy(actor);
        repository.save(entity);
        SettlementResponse after = toResponse(entity);
        audit.record("PARTNER_SETTLEMENT", publicId, "SETTLEMENT_DISABLED", before, after, actor);
        events.enqueue("PARTNER_SETTLEMENT", publicId, "SETTLEMENT_DISABLED", TOPIC, after, actor);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<SettlementResponse> search(String partnerPublicId, String campaignPublicId,
                                                     SettlementStatus status, Pageable pageable) {
        Page<PartnerSettlement> page = repository.findAll(
                PartnerSpecifications.settlementSearch(partnerPublicId, campaignPublicId, status), pageable);
        return new PagedResponse<>(page.getContent().stream().map(this::toResponse).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public SettlementResponse detail(String publicId) {
        return toResponse(repository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> notFound("Settlement not found")));
    }

    private Aggregate aggregate(String campaignPublicId, String partnerPublicId,
                                Instant from, Instant to) {
        BigDecimal coupon;
        BigDecimal voucher;
        long count;
        if (campaignPublicId == null || campaignPublicId.isBlank()) {
            coupon = nz(couponRedemptions.sumConfirmedDiscountForPartner(
                    partnerPublicId, from, to, CONFIRMED));
            voucher = nz(voucherRedemptions.sumConfirmedDiscountForPartner(
                    partnerPublicId, from, to, CONFIRMED));
            count = couponRedemptions.countConfirmedForPartner(partnerPublicId, from, to, CONFIRMED)
                    + voucherRedemptions.countConfirmedForPartner(partnerPublicId, from, to, CONFIRMED);
        } else {
            coupon = nz(couponRedemptions.sumConfirmedDiscount(campaignPublicId, from, to, CONFIRMED));
            voucher = nz(voucherRedemptions.sumConfirmedDiscount(campaignPublicId, from, to, CONFIRMED));
            count = couponRedemptions.countConfirmed(campaignPublicId, from, to, CONFIRMED)
                    + voucherRedemptions.countConfirmed(campaignPublicId, from, to, CONFIRMED);
        }
        return new Aggregate((int) Math.min(Integer.MAX_VALUE, count), coupon.add(voucher));
    }

    private BigDecimal calculatePartnerAmount(PartnerSettlement entity, Aggregate aggregate) {
        if (entity.getSettlementRule() == SettlementRule.FIXED_AMOUNT_PER_REDEMPTION) {
            return nz(entity.getFixedAmountPerRedemption())
                    .multiply(BigDecimal.valueOf(aggregate.orders())).setScale(2, RoundingMode.HALF_UP);
        }
        if (entity.getSettlementRule() == SettlementRule.FULL_PARTNER_FUNDED) {
            return aggregate.discount().setScale(2, RoundingMode.HALF_UP);
        }
        return aggregate.discount().multiply(entity.getPartnerPercentage())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private void transition(PartnerSettlement entity, SettlementStatus target) {
        SettlementStatus current = entity.getStatus();
        boolean allowed = switch (current) {
            case DRAFT -> target == SettlementStatus.PENDING_APPROVAL || target == SettlementStatus.CANCELLED;
            case PENDING_APPROVAL -> target == SettlementStatus.APPROVED
                    || target == SettlementStatus.DISPUTED || target == SettlementStatus.CANCELLED;
            case APPROVED -> target == SettlementStatus.PAID
                    || target == SettlementStatus.CANCELLED
                    || target == SettlementStatus.DISPUTED;
            case DISPUTED -> target == SettlementStatus.APPROVED
                    || target == SettlementStatus.CANCELLED;
            case PAID -> target == SettlementStatus.COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
        if (!allowed) throw bad("Invalid settlement status transition");
        entity.setStatus(target);
        if (target == SettlementStatus.APPROVED) entity.setApprovedAt(Instant.now());
        if (target == SettlementStatus.PAID) entity.setPaidAt(Instant.now());
    }

    private void validateRule(SettlementRule rule, BigDecimal percentage, BigDecimal fixed) {
        if (rule == SettlementRule.FIXED_AMOUNT_PER_REDEMPTION && (fixed == null || fixed.signum() < 0)) {
            throw bad("fixedAmountPerRedemption is required for fixed settlement");
        }
        if (rule != SettlementRule.FIXED_AMOUNT_PER_REDEMPTION
                && (percentage == null || percentage.signum() < 0 || percentage.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw bad("partnerPercentage must be between 0 and 100");
        }
    }

    private void requirePartner(String id) {
        partnerRepository.findByPublicIdAndDeletedAtIsNull(id)
                .filter(partner -> partner.getStatus() == PartnerStatus.ACTIVE)
                .orElseThrow(() -> notFound("Partner not found"));
    }

    private String nextCode(String partner, Instant from) {
        String base = "SET-" + partner.substring(0, Math.min(8, partner.length())).toUpperCase()
                + "-" + from.toString().replaceAll("[^0-9]", "").substring(0, 12);
        String code = base;
        int suffix = 1;
        while (repository.existsBySettlementCode(code)) code = base + "-" + suffix++;
        return code;
    }

    private SettlementResponse toResponse(PartnerSettlement e) {
        SettlementResponse r = new SettlementResponse();
        r.setPublicId(e.getPublicId()); r.setPartnerPublicId(e.getPartnerPublicId());
        r.setCampaignPublicId(e.getCampaignPublicId()); r.setSettlementCode(e.getSettlementCode());
        r.setSettlementPeriodFrom(e.getSettlementPeriodFrom()); r.setSettlementPeriodTo(e.getSettlementPeriodTo());
        r.setTotalOrders(e.getTotalOrders()); r.setTotalDiscount(e.getTotalDiscount());
        r.setPartnerAmount(e.getPartnerAmount()); r.setPlatformAmount(e.getPlatformAmount());
        r.setAdjustmentAmount(e.getAdjustmentAmount()); r.setFinalAmount(e.getFinalAmount());
        r.setCurrency(e.getCurrency()); r.setSettlementRule(e.getSettlementRule());
        r.setPartnerPercentage(e.getPartnerPercentage()); r.setFixedAmountPerRedemption(e.getFixedAmountPerRedemption());
        r.setStatus(e.getStatus()); r.setApprovedAt(e.getApprovedAt()); r.setPaidAt(e.getPaidAt());
        r.setNote(e.getNote()); r.setMetadataJson(e.getMetadataJson()); r.setVersion(e.getVersion());
        r.setCreatedAt(e.getCreatedAt()); r.setCreatedBy(e.getCreatedBy());
        r.setUpdatedAt(e.getUpdatedAt()); r.setUpdatedBy(e.getUpdatedBy());
        return r;
    }

    private static BigDecimal nz(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private static BigDecimal scale(BigDecimal value) { return nz(value).setScale(2, RoundingMode.HALF_UP); }
    private static BusinessException bad(String message) {
        return new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, message, HttpStatus.BAD_REQUEST);
    }
    private static BusinessException notFound(String message) {
        return new BusinessException(ErrorCode.NOT_FOUND, message, HttpStatus.NOT_FOUND);
    }
    private record Aggregate(int orders, BigDecimal discount) {}
}
