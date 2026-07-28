package com.project.promotionservice.benefit.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public final class RedemptionRequests {

    private RedemptionRequests() {
    }

    public static class BenefitValidationRequest {

        @NotBlank
        @Size(max = 100)
        private String code;

        @NotBlank
        @Size(max = 36)
        private String userPublicId;

        @NotNull
        @DecimalMin("0.00")
        private BigDecimal originalAmount;

        @Size(max = 20)
        private String customerPhone;

        @Size(max = 36)
        private String reservationPublicId;

        @Size(max = 36)
        private String bookingPublicId;

        @Size(max = 36)
        private String orderPublicId;

        @Size(max = 36)
        private String paymentPublicId;

        private JsonNode contextJson;

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

}
