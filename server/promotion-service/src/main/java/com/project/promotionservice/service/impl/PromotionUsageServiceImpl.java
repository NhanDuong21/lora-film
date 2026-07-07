package com.project.promotionservice.service.impl;

import com.project.promotionservice.dto.ConfirmUsageRequest;
import com.project.promotionservice.dto.PromotionUsageResponse;
import com.project.promotionservice.dto.RevertUsageRequest;
import com.project.promotionservice.entity.PromotionUsage;
import com.project.promotionservice.enums.PromotionUsageStatus;
import com.project.promotionservice.exception.BusinessException;
import com.project.promotionservice.repository.PromotionRepository;
import com.project.promotionservice.repository.PromotionUsageRepository;
import com.project.promotionservice.service.PromotionUsageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class PromotionUsageServiceImpl implements PromotionUsageService {

    private static final Logger logger = LoggerFactory.getLogger(PromotionUsageServiceImpl.class);

    private final PromotionUsageRepository promotionUsageRepository;
    private final PromotionRepository promotionRepository;
    private final Clock clock;

    public PromotionUsageServiceImpl(PromotionUsageRepository promotionUsageRepository,
                                     PromotionRepository promotionRepository,
                                     Clock clock) {
        this.promotionUsageRepository = promotionUsageRepository;
        this.promotionRepository = promotionRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PromotionUsageResponse confirmUsage(Long usageId, ConfirmUsageRequest request) {
        Objects.requireNonNull(usageId, "usageId must not be null");
        try {
            return executeConfirm(usageId, request);
        } catch (ObjectOptimisticLockingFailureException | jakarta.persistence.OptimisticLockException ex) {
            PromotionUsage usage = promotionUsageRepository.findById(usageId)
                    .orElseThrow(() -> new BusinessException("No promotion usage found", "PROMOTION_USAGE_NOT_FOUND", HttpStatus.NOT_FOUND));
            if (usage.getStatus() == PromotionUsageStatus.APPLIED && usage.getBookingId().equals(request.getBookingId())) {
                logger.info("Idempotent success after concurrent confirm conflict. UsageId: {}, BookingId: {}", usageId, request.getBookingId());
                PromotionUsageResponse response = mapToResponse(usage);
                response.setIdempotent(true);
                return response;
            }
            throw ex;
        }
    }

    private PromotionUsageResponse executeConfirm(Long usageId, ConfirmUsageRequest request) {
        Objects.requireNonNull(usageId, "usageId must not be null");
        PromotionUsage usage = promotionUsageRepository.findById(usageId)
                .orElseThrow(() -> new BusinessException("No promotion usage found", "PROMOTION_USAGE_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!usage.getBookingId().equals(request.getBookingId())) {
            throw new BusinessException("Booking ID mismatch", "PROMOTION_BOOKING_MISMATCH", HttpStatus.CONFLICT);
        }

        PromotionUsageStatus oldStatus = usage.getStatus();
        if (oldStatus == PromotionUsageStatus.APPLIED) {
            logger.info("Idempotent confirm: already APPLIED. UsageId: {}", usageId);
            PromotionUsageResponse response = mapToResponse(usage);
            response.setIdempotent(true);
            return response;
        }

        if (oldStatus == PromotionUsageStatus.REVERTED) {
            throw new BusinessException("Cannot confirm a reverted promotion", "PROMOTION_USAGE_INVALID_TRANSITION", HttpStatus.CONFLICT);
        }

        usage.setStatus(PromotionUsageStatus.APPLIED);
        usage.setConfirmedAt(request.getConfirmedAt() != null ? request.getConfirmedAt() : LocalDateTime.now(clock));
        usage.setRevertedAt(null);
        usage.setRevertReason(null);

        PromotionUsage saved = promotionUsageRepository.saveAndFlush(usage);
        logger.info("Confirmed promotion usage. UsageId: {}, PromotionId: {}, BookingId: {}, oldStatus: {}, newStatus: {}, duration: {}ms",
                usageId, usage.getPromotion().getId(), usage.getBookingId(), oldStatus, saved.getStatus(), 0);

        PromotionUsageResponse response = mapToResponse(saved);
        response.setIdempotent(false);
        return response;
    }

    @Override
    @Transactional
    public PromotionUsageResponse revertUsage(Long usageId, RevertUsageRequest request) {
        Objects.requireNonNull(usageId, "usageId must not be null");
        if (request.getReason() == null) {
            throw new BusinessException("Revert reason is required", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }
        String trimmedReason = request.getReason().trim();
        if (trimmedReason.isEmpty() || trimmedReason.length() > 255) {
            throw new BusinessException("Revert reason must be between 1 and 255 characters", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }

        try {
            return executeRevert(usageId, request, trimmedReason);
        } catch (ObjectOptimisticLockingFailureException | jakarta.persistence.OptimisticLockException ex) {
            PromotionUsage usage = promotionUsageRepository.findById(usageId)
                    .orElseThrow(() -> new BusinessException("No promotion usage found", "PROMOTION_USAGE_NOT_FOUND", HttpStatus.NOT_FOUND));
            if (usage.getStatus() == PromotionUsageStatus.REVERTED && usage.getBookingId().equals(request.getBookingId())) {
                logger.info("Idempotent success after concurrent revert conflict. UsageId: {}, BookingId: {}", usageId, request.getBookingId());
                PromotionUsageResponse response = mapToResponse(usage);
                response.setIdempotent(true);
                return response;
            }
            throw ex;
        }
    }

    private PromotionUsageResponse executeRevert(Long usageId, RevertUsageRequest request, String trimmedReason) {
        Objects.requireNonNull(usageId, "usageId must not be null");
        PromotionUsage usage = promotionUsageRepository.findById(usageId)
                .orElseThrow(() -> new BusinessException("No promotion usage found", "PROMOTION_USAGE_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!usage.getBookingId().equals(request.getBookingId())) {
            throw new BusinessException("Booking ID mismatch", "PROMOTION_BOOKING_MISMATCH", HttpStatus.CONFLICT);
        }

        PromotionUsageStatus oldStatus = usage.getStatus();
        if (oldStatus == PromotionUsageStatus.REVERTED) {
            logger.info("Idempotent revert: already REVERTED. UsageId: {}", usageId);
            PromotionUsageResponse response = mapToResponse(usage);
            response.setIdempotent(true);
            return response;
        }

        if (oldStatus == PromotionUsageStatus.APPLIED) {
            throw new BusinessException("Cannot revert an applied promotion", "PROMOTION_USAGE_INVALID_TRANSITION", HttpStatus.CONFLICT);
        }

        int affectedRows = promotionRepository.decrementUsedCountIfPositive(usage.getPromotion().getId());
        if (affectedRows == 0) {
            logger.warn("Promotion usedCount could not be decremented (already 0). PromotionId: {}, UsageId: {}", usage.getPromotion().getId(), usageId);
        }

        usage.setStatus(PromotionUsageStatus.REVERTED);
        usage.setRevertedAt(LocalDateTime.now(clock));
        usage.setRevertReason(trimmedReason);
        usage.setConfirmedAt(null);

        PromotionUsage saved = promotionUsageRepository.saveAndFlush(usage);
        logger.info("Reverted promotion usage. UsageId: {}, PromotionId: {}, BookingId: {}, oldStatus: {}, newStatus: {}, revertReason: {}, duration: {}ms",
                usageId, usage.getPromotion().getId(), usage.getBookingId(), oldStatus, saved.getStatus(), trimmedReason, 0);

        PromotionUsageResponse response = mapToResponse(saved);
        response.setIdempotent(false);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionUsageResponse getUsageByBookingId(Long bookingId) {
        Objects.requireNonNull(bookingId, "bookingId must not be null");
        PromotionUsage usage = promotionUsageRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new BusinessException("No promotion usage found for this booking", "PROMOTION_USAGE_NOT_FOUND", HttpStatus.NOT_FOUND));

        return mapToResponse(usage);
    }

    private PromotionUsageResponse mapToResponse(PromotionUsage usage) {
        return PromotionUsageResponse.builder()
                .usageId(usage.getId())
                .promotionId(usage.getPromotion().getId())
                .promotionCode(usage.getPromotion().getPromotionCode())
                .bookingId(usage.getBookingId())
                .userId(usage.getUserId())
                .status(usage.getStatus().name())
                .originalAmount(usage.getOriginalAmount())
                .discountAmount(usage.getDiscountAmount())
                .finalAmount(usage.getFinalAmount())
                .expiresAt(usage.getExpiresAt())
                .reservedAt(usage.getReservedAt())
                .confirmedAt(usage.getConfirmedAt())
                .revertedAt(usage.getRevertedAt())
                .revertReason(usage.getRevertReason())
                .build();
    }
}
