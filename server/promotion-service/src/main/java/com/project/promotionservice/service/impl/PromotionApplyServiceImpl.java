package com.project.promotionservice.service.impl;

import com.project.promotionservice.dto.ApplyPromotionRequest;
import com.project.promotionservice.dto.ApplyPromotionResponse;
import com.project.promotionservice.entity.Promotion;
import com.project.promotionservice.entity.PromotionCampaign;
import com.project.promotionservice.entity.PromotionUsage;
import com.project.promotionservice.enums.PromotionUsageStatus;
import com.project.promotionservice.exception.BusinessException;
import com.project.promotionservice.repository.PromotionRepository;
import com.project.promotionservice.repository.PromotionUsageRepository;
import com.project.promotionservice.service.PromotionApplyService;
import com.project.promotionservice.service.booking.BookingContext;
import com.project.promotionservice.service.booking.BookingInternalClient;
import com.project.promotionservice.util.PromotionCodeNormalizer;
import com.project.promotionservice.util.PromotionDiscountCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PromotionApplyServiceImpl implements PromotionApplyService {

    private static final Logger logger = LoggerFactory.getLogger(PromotionApplyServiceImpl.class);

    private final PromotionRepository promotionRepository;
    private final PromotionUsageRepository promotionUsageRepository;
    private final BookingInternalClient bookingInternalClient;
    private final TransactionTemplate transactionTemplate;

    public PromotionApplyServiceImpl(PromotionRepository promotionRepository,
                                     PromotionUsageRepository promotionUsageRepository,
                                     BookingInternalClient bookingInternalClient,
                                     PlatformTransactionManager transactionManager) {
        this.promotionRepository = promotionRepository;
        this.promotionUsageRepository = promotionUsageRepository;
        this.bookingInternalClient = bookingInternalClient;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public ApplyPromotionResponse applyPromotion(ApplyPromotionRequest request) {
        long startTime = System.currentTimeMillis();
        String normalizedCode = PromotionCodeNormalizer.normalize(request.getPromotionCode());
        Long bookingId = request.getBookingId();
        Long userId = request.getUserId();

        logger.info("Applying promotion. PromotionCode: {}, BookingId: {}, UserId: {}", normalizedCode, bookingId, userId);

        // 1. Initial non-transactional check for idempotency & existing usage
        Optional<PromotionUsage> existingUsageOpt = promotionUsageRepository.findByBookingId(bookingId);
        if (existingUsageOpt.isPresent()) {
            PromotionUsage existingUsage = existingUsageOpt.get();
            Promotion promotion = existingUsage.getPromotion();
            
            if (promotion.getPromotionCode().equalsIgnoreCase(normalizedCode)) {
                if (existingUsage.getStatus() == PromotionUsageStatus.RESERVED ||
                        existingUsage.getStatus() == PromotionUsageStatus.APPLIED) {
                    
                    logger.info("Idempotent check: same booking and promotion already applied. BookingId: {}, PromotionCode: {}", bookingId, normalizedCode);
                    return ApplyPromotionResponse.builder()
                            .usageId(existingUsage.getId())
                            .promotionId(promotion.getId())
                            .promotionCode(promotion.getPromotionCode())
                            .bookingId(bookingId)
                            .userId(userId)
                            .originalAmount(existingUsage.getOriginalAmount())
                            .discountAmount(existingUsage.getDiscountAmount())
                            .finalAmount(existingUsage.getFinalAmount())
                            .usageStatus(existingUsage.getStatus().name())
                            .expiresAt(existingUsage.getExpiresAt())
                            .reservedAt(existingUsage.getReservedAt())
                            .confirmedAt(existingUsage.getConfirmedAt())
                            .revertedAt(existingUsage.getRevertedAt())
                            .revertReason(existingUsage.getRevertReason())
                            .idempotent(true)
                            .build();
                } else if (existingUsage.getStatus() == PromotionUsageStatus.REVERTED) {
                    logger.error("Failed to apply promotion. BookingId: {}, ErrorCode: {}", bookingId, "PROMOTION_USAGE_ALREADY_REVERTED");
                    throw new BusinessException("Promotion usage was already reverted", "PROMOTION_USAGE_ALREADY_REVERTED", HttpStatus.CONFLICT);
                }
            } else {
                logger.error("Failed to apply promotion. BookingId: {}, ErrorCode: {}", bookingId, "PROMOTION_BOOKING_ALREADY_APPLIED");
                throw new BusinessException("Booking already has another promotion applied", "PROMOTION_BOOKING_ALREADY_APPLIED", HttpStatus.CONFLICT);
            }
        }

        // 2. Validate booking context internally from booking-service
        BookingContext bookingContext = bookingInternalClient.getBookingContext(bookingId);
        if (bookingContext == null) {
            logger.error("Failed to apply promotion. BookingId: {}, ErrorCode: {}", bookingId, "PROMOTION_BOOKING_NOT_FOUND");
            throw new BusinessException("Booking not found", "PROMOTION_BOOKING_NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        if (!bookingContext.getUserId().equals(userId)) {
            logger.error("Failed to apply promotion. BookingId: {}, ErrorCode: {}", bookingId, "PROMOTION_BOOKING_OWNERSHIP_MISMATCH");
            throw new BusinessException("Booking does not belong to the user", "PROMOTION_BOOKING_OWNERSHIP_MISMATCH", HttpStatus.CONFLICT);
        }

        if (!"PENDING_PAYMENT".equals(bookingContext.getStatus())) {
            logger.error("Failed to apply promotion. BookingId: {}, ErrorCode: {}", bookingId, "PROMOTION_BOOKING_NOT_ELIGIBLE");
            throw new BusinessException("Booking is not in PENDING_PAYMENT status", "PROMOTION_BOOKING_NOT_ELIGIBLE", HttpStatus.CONFLICT);
        }

        LocalDateTime now = LocalDateTime.now();
        if (bookingContext.getExpiresAt().isBefore(now) || request.getBookingExpiresAt().isBefore(now)) {
            logger.error("Failed to apply promotion. BookingId: {}, ErrorCode: {}", bookingId, "PROMOTION_BOOKING_NOT_ELIGIBLE");
            throw new BusinessException("Booking has expired", "PROMOTION_BOOKING_NOT_ELIGIBLE", HttpStatus.CONFLICT);
        }

        if (bookingContext.getAmount().compareTo(request.getBookingAmount()) != 0) {
            logger.error("Failed to apply promotion. BookingId: {}, ErrorCode: {}", bookingId, "PROMOTION_BOOKING_NOT_ELIGIBLE");
            throw new BusinessException("Booking amount mismatch", "PROMOTION_BOOKING_NOT_ELIGIBLE", HttpStatus.CONFLICT);
        }

        // 3. Find Promotion and Campaign
        Promotion promotion = promotionRepository.findByPromotionCodeIgnoreCase(normalizedCode)
                .orElseThrow(() -> {
                    logger.error("Failed to apply promotion. BookingId: {}, ErrorCode: {}", bookingId, "PROMOTION_NOT_FOUND");
                    return new BusinessException("Promotion not found", "PROMOTION_NOT_FOUND", HttpStatus.NOT_FOUND);
                });

        PromotionCampaign campaign = promotion.getCampaign();
        if (campaign == null) {
            logger.error("Failed to apply promotion. BookingId: {}, ErrorCode: {}", bookingId, "PROMOTION_NOT_FOUND");
            throw new BusinessException("Campaign not found for the promotion", "PROMOTION_NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        // 4. Validate Campaign
        if (!campaign.isActive()) {
            logger.error("Failed to apply promotion. BookingId: {}, ErrorCode: {}", bookingId, "CAMPAIGN_DISABLED");
            throw new BusinessException("Campaign is disabled", "CAMPAIGN_DISABLED", HttpStatus.CONFLICT);
        }
        if (now.isBefore(campaign.getStartDate())) {
            logger.error("Failed to apply promotion. BookingId: {}, ErrorCode: {}", bookingId, "CAMPAIGN_NOT_STARTED");
            throw new BusinessException("Campaign has not started yet", "CAMPAIGN_NOT_STARTED", HttpStatus.CONFLICT);
        }
        if (now.isAfter(campaign.getEndDate())) {
            logger.error("Failed to apply promotion. BookingId: {}, ErrorCode: {}", bookingId, "CAMPAIGN_EXPIRED");
            throw new BusinessException("Campaign has expired", "CAMPAIGN_EXPIRED", HttpStatus.CONFLICT);
        }

        // 5. Validate Promotion
        if (!promotion.isActive()) {
            logger.error("Failed to apply promotion. BookingId: {}, ErrorCode: {}", bookingId, "PROMOTION_DISABLED");
            throw new BusinessException("Promotion is disabled", "PROMOTION_DISABLED", HttpStatus.CONFLICT);
        }
        if (now.isBefore(promotion.getStartDate())) {
            logger.error("Failed to apply promotion. BookingId: {}, ErrorCode: {}", bookingId, "PROMOTION_NOT_STARTED");
            throw new BusinessException("Promotion has not started yet", "PROMOTION_NOT_STARTED", HttpStatus.CONFLICT);
        }
        if (now.isAfter(promotion.getEndDate())) {
            logger.error("Failed to apply promotion. BookingId: {}, ErrorCode: {}", bookingId, "PROMOTION_EXPIRED");
            throw new BusinessException("Promotion has expired", "PROMOTION_EXPIRED", HttpStatus.CONFLICT);
        }
        if (promotion.getUsedCount() >= promotion.getUsageLimit()) {
            logger.error("Failed to apply promotion. BookingId: {}, ErrorCode: {}", bookingId, "PROMOTION_USAGE_LIMIT_REACHED");
            throw new BusinessException("Promotion usage limit reached", "PROMOTION_USAGE_LIMIT_REACHED", HttpStatus.CONFLICT);
        }

        // 6. Validate per-user limit
        long userUsageCount = promotionUsageRepository.countByPromotionIdAndUserIdAndStatusIn(
                promotion.getId(),
                userId,
                List.of(PromotionUsageStatus.RESERVED, PromotionUsageStatus.APPLIED)
        );
        if (userUsageCount >= promotion.getLimitPerUser()) {
            logger.error("Failed to apply promotion. BookingId: {}, ErrorCode: {}", bookingId, "PROMOTION_USER_LIMIT_REACHED");
            throw new BusinessException("User usage limit reached for this promotion", "PROMOTION_USER_LIMIT_REACHED", HttpStatus.CONFLICT);
        }

        // 7. Validate minimum booking amount
        if (request.getBookingAmount().compareTo(promotion.getMinOrderAmount()) < 0) {
            logger.error("Failed to apply promotion. BookingId: {}, ErrorCode: {}", bookingId, "PROMOTION_MINIMUM_AMOUNT_NOT_MET");
            throw new BusinessException("Minimum order amount not met", "PROMOTION_MINIMUM_AMOUNT_NOT_MET", HttpStatus.CONFLICT,
                    Map.of("minimumAmount", promotion.getMinOrderAmount(), "currentAmount", request.getBookingAmount()));
        }

        // 8. Calculate discount
        PromotionDiscountCalculator.CalculationResult calcResult = PromotionDiscountCalculator.calculate(
                request.getBookingAmount(),
                promotion.getDiscountType(),
                promotion.getDiscountValue(),
                promotion.getMaxDiscountAmount()
        );

        // 9. Execute transaction: Atomically increment usedCount and Insert PromotionUsage
        try {
            PromotionUsage savedUsage = transactionTemplate.execute(status -> {
                int updatedRows = promotionRepository.incrementUsedCountIfAvailable(promotion.getId());
                if (updatedRows == 0) {
                    throw new BusinessException("Promotion usage limit reached during concurrent reservation", "PROMOTION_USAGE_LIMIT_REACHED", HttpStatus.CONFLICT);
                }

                PromotionUsage promotionUsage = PromotionUsage.builder()
                        .promotion(promotion)
                        .userId(userId)
                        .bookingId(bookingId)
                        .status(PromotionUsageStatus.RESERVED)
                        .originalAmount(request.getBookingAmount())
                        .discountAmount(calcResult.getDiscountAmount())
                        .finalAmount(calcResult.getFinalAmount())
                        .expiresAt(request.getBookingExpiresAt())
                        .build();

                return promotionUsageRepository.saveAndFlush(promotionUsage);
            });

            if (savedUsage == null) {
                throw new BusinessException("Failed to save promotion usage", "INTERNAL_SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Successfully applied promotion. UsageId: {}, PromotionId: {}, BookingId: {}, UserId: {}, OriginalAmount: {}, DiscountAmount: {}, FinalAmount: {}, Idempotent: false, Duration: {}ms",
                    savedUsage.getId(), promotion.getId(), bookingId, userId, request.getBookingAmount(), calcResult.getDiscountAmount(), calcResult.getFinalAmount(), duration);

            return ApplyPromotionResponse.builder()
                    .usageId(savedUsage.getId())
                    .promotionId(promotion.getId())
                    .promotionCode(promotion.getPromotionCode())
                    .bookingId(bookingId)
                    .userId(userId)
                    .originalAmount(request.getBookingAmount())
                    .discountAmount(calcResult.getDiscountAmount())
                    .finalAmount(calcResult.getFinalAmount())
                    .usageStatus(savedUsage.getStatus().name())
                    .expiresAt(savedUsage.getExpiresAt())
                    .reservedAt(savedUsage.getReservedAt())
                    .confirmedAt(savedUsage.getConfirmedAt())
                    .revertedAt(savedUsage.getRevertedAt())
                    .revertReason(savedUsage.getRevertReason())
                    .idempotent(false)
                    .build();

        } catch (DataIntegrityViolationException dive) {
            // Concurrent race condition handling: check if another thread won the insert for this bookingId
            logger.warn("Data integrity violation during apply. Fetching existing usage for bookingId: {}", bookingId);
            Optional<PromotionUsage> reloadUsageOpt = promotionUsageRepository.findByBookingId(bookingId);
            if (reloadUsageOpt.isPresent()) {
                PromotionUsage reloadUsage = reloadUsageOpt.get();
                Promotion relPromo = reloadUsage.getPromotion();
                if (relPromo.getPromotionCode().equalsIgnoreCase(normalizedCode)) {
                    logger.info("Idempotent check on duplicate race recovery: same booking already applied. BookingId: {}, PromotionCode: {}", bookingId, normalizedCode);
                    return ApplyPromotionResponse.builder()
                            .usageId(reloadUsage.getId())
                            .promotionId(relPromo.getId())
                            .promotionCode(relPromo.getPromotionCode())
                            .bookingId(bookingId)
                            .userId(userId)
                            .originalAmount(reloadUsage.getOriginalAmount())
                            .discountAmount(reloadUsage.getDiscountAmount())
                            .finalAmount(reloadUsage.getFinalAmount())
                            .usageStatus(reloadUsage.getStatus().name())
                            .expiresAt(reloadUsage.getExpiresAt())
                            .reservedAt(reloadUsage.getReservedAt())
                            .confirmedAt(reloadUsage.getConfirmedAt())
                            .revertedAt(reloadUsage.getRevertedAt())
                            .revertReason(reloadUsage.getRevertReason())
                            .idempotent(true)
                            .build();
                } else {
                    logger.error("Failed to apply promotion on duplicate race recovery. BookingId: {}, ErrorCode: {}", bookingId, "PROMOTION_BOOKING_ALREADY_APPLIED");
                    throw new BusinessException("Booking already has another promotion applied", "PROMOTION_BOOKING_ALREADY_APPLIED", HttpStatus.CONFLICT);
                }
            }
            throw dive;
        } catch (ObjectOptimisticLockingFailureException | jakarta.persistence.OptimisticLockException ole) {
            logger.error("Optimistic locking conflict applying promotion. BookingId: {}", bookingId);
            throw new BusinessException("Promotion was modified by another transaction", "PROMOTION_OPTIMISTIC_LOCK_CONFLICT", HttpStatus.CONFLICT);
        }
    }
}
