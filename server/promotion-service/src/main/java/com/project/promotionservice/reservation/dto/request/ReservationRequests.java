package com.project.promotionservice.reservation.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.promotionservice.promotion.dto.request.PromotionCheckoutRequest;
import com.project.promotionservice.reservation.enums.ReleaseReasonType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static com.project.promotionservice.common.constant.ValidationConstants.USER_REFERENCE_PATTERN;
import static com.project.promotionservice.common.constant.ValidationConstants.UUID_PATTERN;

public final class ReservationRequests {

    private ReservationRequests() {
    }

    public record ReserveRequest(
            @NotBlank
            @Pattern(regexp = USER_REFERENCE_PATTERN)
            String userPublicId,
            @NotNull @DecimalMin("0.01") BigDecimal originalAmount,
            @Size(max = 1)
            List<@Pattern(regexp = UUID_PATTERN) String> selectedUserPromotionPublicIds,
            @Size(max = 1)
            List<@Pattern(regexp = UUID_PATTERN) String> selectedPromotionPublicIds,
            @Size(max = 100)
            @Pattern(regexp = "^[A-Za-z0-9_-]*$") String couponCode,
            @Size(max = 20) String customerPhone,
            @Pattern(regexp = UUID_PATTERN) String bookingPublicId,
            @Pattern(regexp = UUID_PATTERN) String orderPublicId,
            @Size(max = 10) String currency,
            JsonNode contextJson,
            @Min(60) @Max(1800) Integer holdDurationSeconds,
            @Size(max = 300)
            List<@Pattern(regexp = UUID_PATTERN) String> evaluationUserPromotionPublicIds,
            @Size(max = 300)
            List<@Pattern(regexp = UUID_PATTERN) String> evaluationPromotionPublicIds) {

        @AssertTrue(message = "At least one bookingPublicId or orderPublicId is required")
        public boolean isTransactionReferencePresent() {
            return notBlank(bookingPublicId) || notBlank(orderPublicId);
        }

        @AssertTrue(message = "Only one voucher or coupon can be selected per booking")
        public boolean isSingleManualSelection() {
            return distinctSize(selectedUserPromotionPublicIds)
                    + distinctSize(selectedPromotionPublicIds)
                    + (couponCode == null || couponCode.isBlank() ? 0 : 1) <= 1;
        }

        public PromotionCheckoutRequest checkoutRequest() {
            return new PromotionCheckoutRequest(
                    userPublicId, originalAmount, selectedUserPromotionPublicIds,
                    selectedPromotionPublicIds, couponCode, customerPhone, bookingPublicId, orderPublicId,
                    currency, contextJson, holdDurationSeconds,
                    evaluationUserPromotionPublicIds, evaluationPromotionPublicIds);
        }
    }

    public record ConfirmRequest(
            @NotBlank
            @Size(max = 36)
            @Pattern(regexp = UUID_PATTERN) String paymentPublicId) {
    }

    public record TransitionRequest(
            ReleaseReasonType releaseReasonType,
            @Size(max = 1000) String reasonDetail,
            @Size(max = 100) String sourceService,
            @Size(max = 100) String sourceReference,
            @Size(max = 255) String reason) {

        /** Backwards-compatible constructor for in-process legacy callers. */
        public TransitionRequest(String reason) {
            this(ReleaseReasonType.SYSTEM_COMPENSATION, reason,
                    "LEGACY_CALLER", null, reason);
        }

        @AssertTrue(message = "releaseReasonType is required")
        public boolean isReasonTypePresent() {
            return releaseReasonType != null || (reason != null && !reason.isBlank());
        }

        public ReleaseReasonType resolvedReasonType() {
            return releaseReasonType == null
                    ? ReleaseReasonType.SYSTEM_COMPENSATION : releaseReasonType;
        }

        public String resolvedReasonDetail() {
            if (reasonDetail != null && !reasonDetail.isBlank()) return reasonDetail.trim();
            return reason == null ? null : reason.trim();
        }

        public String resolvedSourceService(String actor) {
            return sourceService == null || sourceService.isBlank()
                    ? actor : sourceService.trim();
        }
    }

    public record CompensateRequest(
            @NotBlank
            @Size(max = 50)
            @Pattern(regexp = "^[A-Z][A-Z0-9_]*$") String reasonCode,
            @NotBlank @Size(max = 255) String reason) {
    }

    public record RefreshRequest(
            @NotNull Instant requestedExpiredAt) {
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static int distinctSize(List<String> values) {
        return values == null ? 0 : (int) values.stream().distinct().count();
    }
}
