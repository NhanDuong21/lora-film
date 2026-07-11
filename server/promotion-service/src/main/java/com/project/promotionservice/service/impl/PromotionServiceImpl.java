package com.project.promotionservice.service.impl;

import com.project.promotionservice.dto.BookingResponse;
import com.project.promotionservice.dto.PromotionResponse;
import com.project.promotionservice.dto.PromotionValidationRequest;
import com.project.promotionservice.dto.PromotionValidationResponse;
import com.project.promotionservice.entity.Promotion;
import com.project.promotionservice.entity.PromotionUsage;
import com.project.promotionservice.enums.DiscountType;
import com.project.promotionservice.enums.PromotionUsageStatus;
import com.project.promotionservice.exception.BusinessException;
import com.project.promotionservice.repository.PromotionRepository;
import com.project.promotionservice.repository.PromotionUsageRepository;
import com.project.promotionservice.service.PromotionAvailabilityService;
import com.project.promotionservice.service.PromotionService;
import com.project.promotionservice.service.booking.BookingServiceClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionUsageRepository promotionUsageRepository;
    private final PromotionAvailabilityService availabilityService;
    private final BookingServiceClient bookingServiceClient;

    public PromotionServiceImpl(PromotionRepository promotionRepository,
                                PromotionUsageRepository promotionUsageRepository,
                                PromotionAvailabilityService availabilityService,
                                BookingServiceClient bookingServiceClient) {
        this.promotionRepository = promotionRepository;
        this.promotionUsageRepository = promotionUsageRepository;
        this.availabilityService = availabilityService;
        this.bookingServiceClient = bookingServiceClient;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PromotionResponse> getActivePromotions(String discountType, BigDecimal minOrderAmount, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        Specification<Promotion> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("active"), true));
            var campaignJoin = root.get("campaign");
            predicates.add(cb.equal(campaignJoin.get("active"), true));

            predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), now));
            predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), now));
            predicates.add(cb.lessThanOrEqualTo(campaignJoin.get("startDate"), now));
            predicates.add(cb.greaterThanOrEqualTo(campaignJoin.get("endDate"), now));

            predicates.add(cb.lessThan(root.get("usedCount"), root.get("usageLimit")));

            if (discountType != null) {
                try {
                    DiscountType type = DiscountType.valueOf(discountType.toUpperCase());
                    predicates.add(cb.equal(root.get("discountType"), type));
                } catch (IllegalArgumentException e) {
                    throw new BusinessException("Invalid discountType: " + discountType,
                            "PROMOTION_INVALID_QUERY", HttpStatus.BAD_REQUEST);
                }
            }

            if (minOrderAmount != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("minOrderAmount"), minOrderAmount));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Promotion> page = promotionRepository.findAll(spec, pageable);
        return page.map(p -> PromotionResponse.builder()
                .promotionId(p.getId())
                .promotionCode(p.getPromotionCode())
                .description(p.getDescription())
                .discountType(p.getDiscountType())
                .discountValue(p.getDiscountValue())
                .maxDiscountAmount(p.getMaxDiscountAmount())
                .minOrderAmount(p.getMinOrderAmount())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .availabilityStatus("ACTIVE")
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionResponse getPromotionDetail(Long id) {
        Promotion p = promotionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Promotion not found", "PROMOTION_NOT_FOUND", HttpStatus.NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        String availabilityStatus = availabilityService.getPromotionStatus(p, now);

        return PromotionResponse.builder()
                .promotionId(p.getId())
                .campaignId(p.getCampaign() != null ? p.getCampaign().getId() : null)
                .campaignName(p.getCampaign() != null ? p.getCampaign().getCampaignName() : null)
                .promotionCode(p.getPromotionCode())
                .description(p.getDescription())
                .discountType(p.getDiscountType())
                .discountValue(p.getDiscountValue())
                .maxDiscountAmount(p.getMaxDiscountAmount())
                .minOrderAmount(p.getMinOrderAmount())
                .limitPerUser(p.getLimitPerUser())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .availabilityStatus(availabilityStatus)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionValidationResponse validatePromotion(PromotionValidationRequest request, String authHeader) {
        return processValidation(request, authHeader, false);
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionValidationResponse previewDiscount(PromotionValidationRequest request, String authHeader) {
        return processValidation(request, authHeader, true);
    }

    private PromotionValidationResponse processValidation(PromotionValidationRequest request, String authHeader, boolean isPreview) {
        String code = request.getPromotionCode() != null ? request.getPromotionCode().trim().toUpperCase() : "";

        // Resolve user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || "anonymous".equalsIgnoreCase(authentication.getName())) {
            throw new BusinessException("User is not authenticated", "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
        Long userId = Long.valueOf(authentication.getName());

        // Get and validate booking
        BookingResponse booking = bookingServiceClient.getBooking(request.getBookingId(), authHeader);
        if (!"PENDING_PAYMENT".equals(booking.getStatus())) {
            throw new BusinessException("Booking is not in PENDING_PAYMENT status", "PROMOTION_BOOKING_NOT_ELIGIBLE", HttpStatus.CONFLICT);
        }

        // Check if booking already has active promotion usage
        Optional<PromotionUsage> activeUsage = promotionUsageRepository.findByBookingId(request.getBookingId());
        if (activeUsage.isPresent() && activeUsage.get().getStatus() != PromotionUsageStatus.REVERTED) {
            throw new BusinessException("Booking already has an applied promotion", "PROMOTION_BOOKING_ALREADY_APPLIED", HttpStatus.CONFLICT);
        }

        // Retrieve promotion
        Promotion promo = promotionRepository.findByPromotionCodeIgnoreCase(code)
                .orElseThrow(() -> new BusinessException("Promotion not found", "PROMOTION_NOT_FOUND", HttpStatus.NOT_FOUND));

        // Validate availability
        LocalDateTime now = LocalDateTime.now();
        String status = availabilityService.getPromotionStatus(promo, now);
        if ("DISABLED".equals(status)) {
            throw new BusinessException("Promotion is disabled", "PROMOTION_DISABLED", HttpStatus.CONFLICT);
        } else if ("UPCOMING".equals(status)) {
            LocalDateTime maxStart = promo.getStartDate();
            if (promo.getCampaign() != null && promo.getCampaign().getStartDate().isAfter(maxStart)) {
                maxStart = promo.getCampaign().getStartDate();
            }
            throw new BusinessException("Promotion has not started yet", "PROMOTION_NOT_STARTED", HttpStatus.CONFLICT, Map.of("startDate", maxStart));
        } else if ("EXPIRED".equals(status)) {
            throw new BusinessException("Promotion has expired", "PROMOTION_EXPIRED", HttpStatus.CONFLICT);
        } else if ("OUT_OF_USAGE".equals(status)) {
            throw new BusinessException("Promotion usage limit has been reached", "PROMOTION_USAGE_LIMIT_REACHED", HttpStatus.CONFLICT);
        }

        // Validate per-user limit
        long userUsageCount = promotionUsageRepository.countByPromotionIdAndUserIdAndStatusIn(
                promo.getId(),
                userId,
                List.of(PromotionUsageStatus.RESERVED, PromotionUsageStatus.APPLIED)
        );
        if (userUsageCount >= promo.getLimitPerUser()) {
            throw new BusinessException("You have reached the usage limit for this promotion", "PROMOTION_USER_LIMIT_REACHED", HttpStatus.CONFLICT);
        }

        // Validate minimum order amount
        BigDecimal bookingAmount = booking.getTotalAmount() != null ? booking.getTotalAmount() : BigDecimal.ZERO;
        if (bookingAmount.compareTo(promo.getMinOrderAmount()) < 0) {
            throw new BusinessException("Booking amount does not meet the promotion minimum",
                    "PROMOTION_MINIMUM_AMOUNT_NOT_MET", HttpStatus.CONFLICT,
                    Map.of("minimumAmount", promo.getMinOrderAmount(), "currentAmount", bookingAmount));
        }

        // Calculate discount
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (promo.getDiscountType() == DiscountType.PERCENTAGE) {
            BigDecimal rawDiscount = bookingAmount.multiply(promo.getDiscountValue()).divide(BigDecimal.valueOf(100));
            discountAmount = rawDiscount.setScale(0, RoundingMode.HALF_UP);
            if (promo.getMaxDiscountAmount() != null && discountAmount.compareTo(promo.getMaxDiscountAmount()) > 0) {
                discountAmount = promo.getMaxDiscountAmount().setScale(0, RoundingMode.HALF_UP);
            }
        } else if (promo.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            discountAmount = promo.getDiscountValue().setScale(0, RoundingMode.HALF_UP);
            if (discountAmount.compareTo(bookingAmount) > 0) {
                discountAmount = bookingAmount.setScale(0, RoundingMode.HALF_UP);
            }
        }
        BigDecimal finalAmount = bookingAmount.subtract(discountAmount).max(BigDecimal.ZERO).setScale(0, RoundingMode.HALF_UP);

        if (isPreview) {
            return PromotionValidationResponse.builder()
                    .promotionId(promo.getId())
                    .promotionCode(promo.getPromotionCode())
                    .originalAmount(bookingAmount)
                    .discountAmount(discountAmount)
                    .finalAmount(finalAmount)
                    .currency("VND")
                    .previewOnly(true)
                    .build();
        } else {
            return PromotionValidationResponse.builder()
                    .valid(true)
                    .promotionId(promo.getId())
                    .promotionCode(promo.getPromotionCode())
                    .bookingId(booking.getBookingId())
                    .originalAmount(bookingAmount)
                    .discountType(promo.getDiscountType())
                    .discountValue(promo.getDiscountValue())
                    .discountAmount(discountAmount)
                    .finalAmount(finalAmount)
                    .expiresAt(booking.getExpiresAt())
                    .build();
        }
    }
}
