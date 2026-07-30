package com.project.promotionservice.benefit.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

public final class RedemptionRequests {

    private static final String UUID_PATTERN =
            com.project.promotionservice.common.constant.ValidationConstants.UUID_PATTERN;
    private static final String USER_REFERENCE_PATTERN =
            com.project.promotionservice.common.constant.ValidationConstants.USER_REFERENCE_PATTERN;

    private RedemptionRequests() {
    }

    public static class BenefitValidationRequest {

        @NotBlank
        @Size(max = 100)
        private String code;

        @NotBlank
        @Size(max = 36)
        @Pattern(regexp = USER_REFERENCE_PATTERN,
                message = "userPublicId must be a positive account ID or a valid UUID")
        private String userPublicId;

        @NotNull
        @DecimalMin("0.01")
        @Digits(integer = 16, fraction = 2)
        private BigDecimal originalAmount;

        @Size(max = 20)
        private String customerPhone;

        @Size(max = 36)
        @Pattern(regexp = UUID_PATTERN, message = "reservationPublicId must be a valid UUID")
        private String reservationPublicId;

        @Size(max = 36)
        @Pattern(regexp = UUID_PATTERN, message = "bookingPublicId must be a valid UUID")
        private String bookingPublicId;

        @Size(max = 36)
        @Pattern(regexp = UUID_PATTERN, message = "orderPublicId must be a valid UUID")
        private String orderPublicId;

        @Size(max = 36)
        @Pattern(regexp = UUID_PATTERN, message = "paymentPublicId must be a valid UUID")
        private String paymentPublicId;

        private JsonNode contextJson;

        @AssertTrue(message = "contextJson must be an object no larger than 16 KiB")
        public boolean isContextJsonValid() {
            return contextJson == null
                    || contextJson.isNull()
                    || (contextJson.isObject()
                    && contextJson.toString().getBytes(StandardCharsets.UTF_8).length <= 16_384);
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getUserPublicId() {
            return userPublicId;
        }

        public void setUserPublicId(String userPublicId) {
            this.userPublicId = userPublicId;
        }

        public BigDecimal getOriginalAmount() {
            return originalAmount;
        }

        public void setOriginalAmount(BigDecimal originalAmount) {
            this.originalAmount = originalAmount;
        }

        public String getCustomerPhone() {
            return customerPhone;
        }

        public void setCustomerPhone(String customerPhone) {
            this.customerPhone = customerPhone;
        }

        public String getReservationPublicId() {
            return reservationPublicId;
        }

        public void setReservationPublicId(String reservationPublicId) {
            this.reservationPublicId = reservationPublicId;
        }

        public String getBookingPublicId() {
            return bookingPublicId;
        }

        public void setBookingPublicId(String bookingPublicId) {
            this.bookingPublicId = bookingPublicId;
        }

        public String getOrderPublicId() {
            return orderPublicId;
        }

        public void setOrderPublicId(String orderPublicId) {
            this.orderPublicId = orderPublicId;
        }

        public String getPaymentPublicId() {
            return paymentPublicId;
        }

        public void setPaymentPublicId(String paymentPublicId) {
            this.paymentPublicId = paymentPublicId;
        }

        public JsonNode getContextJson() {
            return contextJson;
        }

        public void setContextJson(JsonNode contextJson) {
            this.contextJson = contextJson;
        }
    }

    public static class BenefitRedeemRequest extends BenefitValidationRequest {

        @AssertTrue(message = "At least one bookingPublicId, orderPublicId or paymentPublicId is required")
        public boolean isTransactionReferencePresent() {
            return notBlank(getBookingPublicId()) || notBlank(getOrderPublicId()) || notBlank(getPaymentPublicId());
        }

        private boolean notBlank(String value) {
            return value != null && !value.isBlank();
        }
    }

    public static class ReservedRedemptionRequest extends BenefitRedeemRequest {

        @NotNull
        @DecimalMin("0.00")
        private BigDecimal discountAmount;

        @NotNull
        @DecimalMin("0.00")
        private BigDecimal finalAmount;

        @NotBlank
        @Size(max = 10)
        private String currency;

        public BigDecimal getDiscountAmount() {
            return discountAmount;
        }

        public void setDiscountAmount(BigDecimal discountAmount) {
            this.discountAmount = discountAmount;
        }

        public BigDecimal getFinalAmount() {
            return finalAmount;
        }

        public void setFinalAmount(BigDecimal finalAmount) {
            this.finalAmount = finalAmount;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }
    }

}
