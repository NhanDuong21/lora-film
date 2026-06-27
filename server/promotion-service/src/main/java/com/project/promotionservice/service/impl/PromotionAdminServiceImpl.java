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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
        long startTime = System.currentTimeMillis();
        String actorId = getCurrentActorId();

        try {
            LocalDateTime now = LocalDateTime.now();
            Specification<Promotion> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();

                if (isActive != null) {
                    predicates.add(cb.equal(root.get("active"), isActive));
                }

                if (campaignId != null) {
                    predicates.add(cb.equal(root.get("campaign").get("id"), campaignId));
                }

                if (discountType != null) {
                    try {
                        DiscountType type = DiscountType.valueOf(discountType.toUpperCase());
                        predicates.add(cb.equal(root.get("discountType"), type));
                    } catch (IllegalArgumentException e) {
                        throw new BusinessException("Invalid discountType: " + discountType,
                                "PROMOTION_INVALID_QUERY", HttpStatus.BAD_REQUEST);
                    }
                }

                if (availabilityStatus != null) {
                    var campaignJoin = root.get("campaign");
                    switch (availabilityStatus.toUpperCase()) {
                        case "DISABLED":
                            predicates.add(cb.or(
                                    cb.equal(root.get("active"), false),
                                    cb.equal(campaignJoin.get("active"), false)
                            ));
                            break;
                        case "UPCOMING":
                            predicates.add(cb.and(
                                    cb.equal(root.get("active"), true),
                                    cb.equal(campaignJoin.get("active"), true),
                                    cb.or(
                                            cb.greaterThan(root.get("startDate"), now),
                                            cb.greaterThan(campaignJoin.get("startDate"), now)
                                    )
                            ));
                            break;
                        case "EXPIRED":
                            predicates.add(cb.and(
                                    cb.equal(root.get("active"), true),
                                    cb.equal(campaignJoin.get("active"), true),
                                    cb.or(
                                            cb.lessThan(root.get("endDate"), now),
                                            cb.lessThan(campaignJoin.get("endDate"), now)
                                    )
                            ));
                            break;
                        case "OUT_OF_USAGE":
                            predicates.add(cb.and(
                                    cb.equal(root.get("active"), true),
                                    cb.equal(campaignJoin.get("active"), true),
                                    cb.ge(root.get("usedCount"), root.get("usageLimit"))
                            ));
                            break;
                        case "ACTIVE":
                            predicates.add(cb.and(
                                    cb.equal(root.get("active"), true),
                                    cb.equal(campaignJoin.get("active"), true),
                                    cb.lessThanOrEqualTo(root.get("startDate"), now),
                                    cb.greaterThanOrEqualTo(root.get("endDate"), now),
                                    cb.lessThanOrEqualTo(campaignJoin.get("startDate"), now),
                                    cb.greaterThanOrEqualTo(campaignJoin.get("endDate"), now),
                                    cb.lessThan(root.get("usedCount"), root.get("usageLimit"))
                            ));
                            break;
                        default:
                            throw new BusinessException("Invalid availabilityStatus: " + availabilityStatus,
                                    "PROMOTION_INVALID_QUERY", HttpStatus.BAD_REQUEST);
                    }
                }

                if (from != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
                }
                if (to != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
                }

                return cb.and(predicates.toArray(new Predicate[0]));
            };

            var page = promotionRepository.findAll(spec, pageable);

            List<PromotionListItemResponse> content = page.getContent().stream()
                    .map(promotion -> PromotionListItemResponse.builder()
                            .promotionId(promotion.getId())
                            .promotionCode(promotion.getPromotionCode())
                            .discountType(promotion.getDiscountType())
                            .discountValue(promotion.getDiscountValue())
                            .usageLimit(promotion.getUsageLimit())
                            .usedCount(promotion.getUsedCount())
                            .startDate(promotion.getStartDate())
                            .endDate(promotion.getEndDate())
                            .isActive(promotion.isActive())
                            .availabilityStatus(availabilityService.getPromotionStatus(promotion, now))
                            .campaignId(promotion.getCampaign().getId())
                            .createdAt(promotion.getCreatedAt())
                            .updatedAt(promotion.getUpdatedAt())
                            .build())
                    .toList();

            long duration = System.currentTimeMillis() - startTime;
            log.info("Actor: {}, Operation: GET_PROMOTIONS, Count: {}, Duration: {}ms",
                    actorId, content.size(), duration);

            return PromotionPageResponse.builder()
                    .content(content)
                    .page(page.getNumber())
                    .size(page.getSize())
                    .totalElements(page.getTotalElements())
                    .totalPages(page.getTotalPages())
                    .first(page.isFirst())
                    .last(page.isLast())
                    .build();

        } catch (BusinessException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Actor: {}, Operation: GET_PROMOTIONS, Duration: {}ms, ErrorCode: {}, Message: {}",
                    actorId, duration, e.getErrorCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Actor: {}, Operation: GET_PROMOTIONS, Duration: {}ms, ErrorCode: INTERNAL_SERVER_ERROR, Message: {}",
                    actorId, duration, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionResponse getPromotionById(Long id) {
        long startTime = System.currentTimeMillis();
        String actorId = getCurrentActorId();

        try {
            Promotion promotion = promotionRepository.findById(id)
                    .orElseThrow(() -> new BusinessException("Promotion not found with id: " + id, "PROMOTION_NOT_FOUND", HttpStatus.NOT_FOUND));

            LocalDateTime now = LocalDateTime.now();
            String availability = availabilityService.getPromotionStatus(promotion, now);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Actor: {}, Operation: GET_PROMOTION_DETAIL, PromotionId: {}, Duration: {}ms",
                    actorId, id, duration);

            return PromotionResponse.builder()
                    .promotionId(promotion.getId())
                    .promotionCode(promotion.getPromotionCode())
                    .description(promotion.getDescription())
                    .discountType(promotion.getDiscountType())
                    .discountValue(promotion.getDiscountValue())
                    .maxDiscountAmount(promotion.getMaxDiscountAmount())
                    .minOrderAmount(promotion.getMinOrderAmount())
                    .usageLimit(promotion.getUsageLimit())
                    .limitPerUser(promotion.getLimitPerUser())
                    .usedCount(promotion.getUsedCount())
                    .startDate(promotion.getStartDate())
                    .endDate(promotion.getEndDate())
                    .isActive(promotion.isActive())
                    .availabilityStatus(availability)
                    .campaignId(promotion.getCampaign().getId())
                    .createdAt(promotion.getCreatedAt())
                    .updatedAt(promotion.getUpdatedAt())
                    .build();

        } catch (BusinessException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Actor: {}, Operation: GET_PROMOTION_DETAIL, PromotionId: {}, Duration: {}ms, ErrorCode: {}, Message: {}",
                    actorId, id, duration, e.getErrorCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Actor: {}, Operation: GET_PROMOTION_DETAIL, PromotionId: {}, Duration: {}ms, ErrorCode: INTERNAL_SERVER_ERROR, Message: {}",
                    actorId, id, duration, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PromotionResponse updatePromotion(Long id, UpdatePromotionRequest request) {
        long startTime = System.currentTimeMillis();
        String actorId = getCurrentActorId();

        try {
            Promotion promotion = promotionRepository.findById(id)
                    .orElseThrow(() -> new BusinessException("Promotion not found with id: " + id, "PROMOTION_NOT_FOUND", HttpStatus.NOT_FOUND));

            // Trim code and uppercase it
            String promotionCode = request.getPromotionCode() != null ? request.getPromotionCode().trim().toUpperCase() : null;
            if (promotionCode == null || promotionCode.isEmpty()) {
                throw new BusinessException("promotionCode is required", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
            }

            // Check uniqueness if changed
            if (!promotion.getPromotionCode().equalsIgnoreCase(promotionCode) && promotionRepository.existsByPromotionCode(promotionCode)) {
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

            // Reject if usageLimit < usedCount
            if (request.getUsageLimit() < promotion.getUsedCount()) {
                throw new BusinessException("usageLimit cannot be smaller than current usedCount: " + promotion.getUsedCount(), "PROMOTION_USAGE_LIMIT_INVALID", HttpStatus.BAD_REQUEST);
            }

            promotion.setPromotionCode(promotionCode);
            promotion.setDescription(request.getDescription());
            promotion.setDiscountType(request.getDiscountType());
            promotion.setDiscountValue(discountValue);
            promotion.setMaxDiscountAmount(maxDiscountAmount);
            promotion.setMinOrderAmount(request.getMinOrderAmount());
            promotion.setUsageLimit(request.getUsageLimit());
            promotion.setLimitPerUser(request.getLimitPerUser());
            promotion.setStartDate(request.getStartDate());
            promotion.setEndDate(request.getEndDate());
            promotion.setCampaign(campaign);
            promotion.setActive(request.getIsActive() != null ? request.getIsActive() : true);

            Promotion savedPromotion = promotionRepository.save(promotion);
            LocalDateTime now = LocalDateTime.now();
            String availability = availabilityService.getPromotionStatus(savedPromotion, now);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Actor: {}, Operation: UPDATE_PROMOTION, PromotionId: {}, Duration: {}ms",
                    actorId, id, duration);

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
            log.error("Actor: {}, Operation: UPDATE_PROMOTION, PromotionId: {}, Duration: {}ms, ErrorCode: {}, Message: {}",
                    actorId, id, duration, e.getErrorCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Actor: {}, Operation: UPDATE_PROMOTION, PromotionId: {}, Duration: {}ms, ErrorCode: INTERNAL_SERVER_ERROR, Message: {}",
                    actorId, id, duration, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PromotionResponse updatePromotionStatus(Long id, UpdatePromotionStatusRequest request) {
        long startTime = System.currentTimeMillis();
        String actorId = getCurrentActorId();

        try {
            Promotion promotion = promotionRepository.findById(id)
                    .orElseThrow(() -> new BusinessException("Promotion not found with id: " + id, "PROMOTION_NOT_FOUND", HttpStatus.NOT_FOUND));

            promotion.setActive(request.getIsActive());
            Promotion savedPromotion = promotionRepository.save(promotion);

            LocalDateTime now = LocalDateTime.now();
            String availability = availabilityService.getPromotionStatus(savedPromotion, now);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Actor: {}, Operation: UPDATE_PROMOTION_STATUS, PromotionId: {}, Status: {}, Duration: {}ms",
                    actorId, id, request.getIsActive(), duration);

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
            log.error("Actor: {}, Operation: UPDATE_PROMOTION_STATUS, PromotionId: {}, Duration: {}ms, ErrorCode: {}, Message: {}",
                    actorId, id, duration, e.getErrorCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Actor: {}, Operation: UPDATE_PROMOTION_STATUS, PromotionId: {}, Duration: {}ms, ErrorCode: INTERNAL_SERVER_ERROR, Message: {}",
                    actorId, id, duration, e.getMessage(), e);
            throw e;
        }
    }
}
