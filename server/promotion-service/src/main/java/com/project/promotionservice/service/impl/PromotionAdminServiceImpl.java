package com.project.promotionservice.service.impl;

import com.project.promotionservice.dto.*;
import com.project.promotionservice.entity.Promotion;
import com.project.promotionservice.entity.PromotionCampaign;
import com.project.promotionservice.enums.DiscountType;
import com.project.promotionservice.exception.BusinessException;
import com.project.promotionservice.repository.PromotionCampaignRepository;
import com.project.promotionservice.repository.PromotionRepository;
import com.project.promotionservice.service.PromotionAvailabilityService;
import com.project.promotionservice.service.PromotionAdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
public class PromotionAdminServiceImpl implements PromotionAdminService {

    private final PromotionRepository promotionRepository;
    private final PromotionCampaignRepository campaignRepository;
    private final PromotionAvailabilityService availabilityService;

    public PromotionAdminServiceImpl(PromotionRepository promotionRepository,
                                     PromotionCampaignRepository campaignRepository,
                                     PromotionAvailabilityService availabilityService) {
        this.promotionRepository = promotionRepository;
        this.campaignRepository = campaignRepository;
        this.availabilityService = availabilityService;
    }

    private String getCurrentActorId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return "anonymous";
        }
        return authentication.getName();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PromotionResponse createPromotion(CreatePromotionRequest request) {
        long startTime = System.currentTimeMillis();
        String actorId = getCurrentActorId();

        try {
            // Trim code and uppercase it
            String promotionCode = request.getPromotionCode() != null ? request.getPromotionCode().trim().toUpperCase() : null;
            if (promotionCode == null || promotionCode.isEmpty()) {
                throw new BusinessException("promotionCode is required", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
            }

            // Check uniqueness
            if (promotionRepository.existsByPromotionCode(promotionCode)) {
                throw new BusinessException("Promotion code already exists", "PROMOTION_CODE_ALREADY_EXISTS", HttpStatus.BAD_REQUEST);
            }

            // Fetch campaign
            PromotionCampaign campaign = campaignRepository.findById(request.getCampaignId())
                    .orElseThrow(() -> new BusinessException("Campaign not found with id: " + request.getCampaignId(), "CAMPAIGN_NOT_FOUND", HttpStatus.NOT_FOUND));

            // Validate date range
            if (request.getEndDate().isBefore(request.getStartDate()) || request.getEndDate().isEqual(request.getStartDate())) {
                throw new BusinessException("endDate must be after startDate", "PROMOTION_INVALID_DATE_RANGE", HttpStatus.BAD_REQUEST);
            }

            // Validate within campaign range
            if (request.getStartDate().isBefore(campaign.getStartDate()) || request.getEndDate().isAfter(campaign.getEndDate())) {
                throw new BusinessException("Promotion dates must be within campaign dates", "PROMOTION_OUTSIDE_CAMPAIGN_RANGE", HttpStatus.BAD_REQUEST);
            }

            // Validate discount
            BigDecimal discountValue = request.getDiscountValue();
            BigDecimal maxDiscountAmount = request.getMaxDiscountAmount();

            if (request.getDiscountType() == DiscountType.PERCENTAGE) {
                if (discountValue.compareTo(BigDecimal.ZERO) <= 0 || discountValue.compareTo(new BigDecimal("100.00")) > 0) {
                    throw new BusinessException("PERCENTAGE discount value must be between 0.01 and 100.00", "PROMOTION_INVALID_DISCOUNT_VALUE", HttpStatus.BAD_REQUEST);
                }
                if (maxDiscountAmount == null) {
                    throw new BusinessException("maxDiscountAmount is required for PERCENTAGE discount type", "PROMOTION_INVALID_DISCOUNT_VALUE", HttpStatus.BAD_REQUEST);
                }
            } else if (request.getDiscountType() == DiscountType.FIXED_AMOUNT) {
                if (discountValue.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessException("FIXED_AMOUNT discount value must be greater than 0", "PROMOTION_INVALID_DISCOUNT_VALUE", HttpStatus.BAD_REQUEST);
                }
                maxDiscountAmount = null; // Ignore or null for fixed amount
            }

            Promotion promotion = Promotion.builder()
                    .promotionCode(promotionCode)
                    .description(request.getDescription())
                    .discountType(request.getDiscountType())
                    .discountValue(discountValue)
                    .maxDiscountAmount(maxDiscountAmount)
                    .minOrderAmount(request.getMinOrderAmount())
                    .usageLimit(request.getUsageLimit())
                    .limitPerUser(request.getLimitPerUser())
                    .startDate(request.getStartDate())
                    .endDate(request.getEndDate())
                    .campaign(campaign)
                    .active(request.getIsActive() != null ? request.getIsActive() : true)
                    .usedCount(0)
                    .build();

            Promotion savedPromotion = promotionRepository.save(promotion);
            LocalDateTime now = LocalDateTime.now();
            String availability = availabilityService.getPromotionStatus(savedPromotion, now);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Actor: {}, Operation: CREATE_PROMOTION, PromotionId: {}, Duration: {}ms",
                    actorId, savedPromotion.getId(), duration);

            return PromotionResponse.builder()
                    .promotionId(savedPromotion.getId())
                    .promotionCode(savedPromotion.getPromotionCode())
                    .description(savedPromotion.getDescription())
                    .discountType(savedPromotion.getDiscountType())
                    .discountValue(savedPromotion.getDiscountValue())
                    .maxDiscountAmount(savedPromotion.getMaxDiscountAmount())
                    .minOrderAmount(savedPromotion.getMinOrderAmount())
                    .usageLimit(savedPromotion.getUsageLimit())
                    .limitPerUser(savedPromotion.getLimitPerUser())
                    .usedCount(savedPromotion.getUsedCount())
                    .startDate(savedPromotion.getStartDate())
                    .endDate(savedPromotion.getEndDate())
                    .isActive(savedPromotion.isActive())
                    .availabilityStatus(availability)
                    .campaignId(savedPromotion.getCampaign().getId())
                    .createdAt(savedPromotion.getCreatedAt())
                    .updatedAt(savedPromotion.getUpdatedAt())
                    .build();

        } catch (BusinessException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Actor: {}, Operation: CREATE_PROMOTION, Duration: {}ms, ErrorCode: {}, Message: {}",
                    actorId, duration, e.getErrorCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Actor: {}, Operation: CREATE_PROMOTION, Duration: {}ms, ErrorCode: INTERNAL_SERVER_ERROR, Message: {}",
                    actorId, duration, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionPageResponse getPromotions(Boolean isActive, String availabilityStatus, String discountType,
                                              Long campaignId, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        // To be implemented in Unit 9
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionResponse getPromotionById(Long id) {
        // To be implemented in Unit 9
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PromotionResponse updatePromotion(Long id, UpdatePromotionRequest request) {
        // To be implemented in Unit 10
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PromotionResponse updatePromotionStatus(Long id, UpdatePromotionStatusRequest request) {
        // To be implemented in Unit 11
        return null;
    }
}
