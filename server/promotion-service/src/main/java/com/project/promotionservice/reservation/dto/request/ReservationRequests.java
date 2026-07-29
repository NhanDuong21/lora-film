package com.project.promotionservice.reservation.dto.request;

import com.project.promotionservice.benefit.dto.request.RedemptionRequests.BenefitValidationRequest;
import com.project.promotionservice.benefit.enums.BenefitEnums.RedemptionType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

import static com.project.promotionservice.common.constant.ValidationConstants.UUID_PATTERN;

public final class ReservationRequests {

    private ReservationRequests() {
    }

    public static class ReserveRequest extends BenefitValidationRequest {

        @NotNull
        private RedemptionType benefitType;

        @Min(60)
        @Max(1800)
        private int holdDurationSeconds = 900;

        @AssertTrue(message = "At least one bookingPublicId or orderPublicId is required")
        public boolean isTransactionReferencePresent() {
            return notBlank(getBookingPublicId()) || notBlank(getOrderPublicId());
        }

        public RedemptionType getBenefitType() {
            return benefitType;
        }

        public void setBenefitType(RedemptionType benefitType) {
            this.benefitType = benefitType;
        }

        public int getHoldDurationSeconds() {
            return holdDurationSeconds;
        }

        public void setHoldDurationSeconds(int holdDurationSeconds) {
            this.holdDurationSeconds = holdDurationSeconds;
        }
    }

    public static class ConfirmRequest {

        @NotBlank
        @Size(max = 36)
        @Pattern(regexp = UUID_PATTERN, message = "paymentPublicId must be a valid UUID")
        private String paymentPublicId;

        public String getPaymentPublicId() {
            return paymentPublicId;
        }

        public void setPaymentPublicId(String paymentPublicId) {
            this.paymentPublicId = paymentPublicId;
        }
    }

    public static class TransitionRequest {
        @NotBlank
        @Size(max = 255)
        private String reason;

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static class RefreshRequest {

        @NotNull
        private Instant requestedExpiredAt;

        public Instant getRequestedExpiredAt() {
            return requestedExpiredAt;
        }

        public void setRequestedExpiredAt(Instant requestedExpiredAt) {
            this.requestedExpiredAt = requestedExpiredAt;
        }
    }

    public static class RuntimeValidationRequest extends BenefitValidationRequest {

        @NotNull
        private RedemptionType benefitType;

        public RedemptionType getBenefitType() {
            return benefitType;
        }

        public void setBenefitType(RedemptionType benefitType) {
            this.benefitType = benefitType;
        }
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
