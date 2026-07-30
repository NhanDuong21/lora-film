package com.project.promotionservice.benefit.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.promotionservice.benefit.enums.BenefitEnums.CompensationStatus;
import com.project.promotionservice.benefit.enums.BenefitEnums.CompensationType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public final class CompensationRequests {

    private CompensationRequests() {
    }

    public static class CompensationIssueRequest {

        @Size(max = 36)
        private String reservationPublicId;

        @Size(max = 36)
        private String bookingPublicId;

        @Size(max = 36)
        private String orderPublicId;

        @NotBlank
        @Size(max = 36)
        @Pattern(
                regexp = com.project.promotionservice.common.constant.ValidationConstants.USER_REFERENCE_PATTERN,
                message = "userPublicId must be a positive account ID or a valid UUID")
        private String userPublicId;

        @NotNull
        private CompensationType compensationType;

        @NotBlank
        @Size(max = 255)
        private String reason;

        @NotNull
        @DecimalMin(value = "0.01")
        private BigDecimal amount;

        @NotNull
        @Future
        private Instant expiredAt;

        @Size(max = 100)
        private String voucherCode;

        @Size(max = 255)
        private String voucherName;

        private JsonNode metadataJson;

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

        public String getUserPublicId() {
            return userPublicId;
        }

        public void setUserPublicId(String userPublicId) {
            this.userPublicId = userPublicId;
        }

        public CompensationType getCompensationType() {
            return compensationType;
        }

        public void setCompensationType(CompensationType compensationType) {
            this.compensationType = compensationType;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public Instant getExpiredAt() {
            return expiredAt;
        }

        public void setExpiredAt(Instant expiredAt) {
            this.expiredAt = expiredAt;
        }

        public String getVoucherCode() {
            return voucherCode;
        }

        public void setVoucherCode(String voucherCode) {
            this.voucherCode = voucherCode;
        }

        public String getVoucherName() {
            return voucherName;
        }

        public void setVoucherName(String voucherName) {
            this.voucherName = voucherName;
        }

        public JsonNode getMetadataJson() {
            return metadataJson;
        }

        public void setMetadataJson(JsonNode metadataJson) {
            this.metadataJson = metadataJson;
        }
    }

    public static class CompensationUpdateRequest {

        @Size(max = 255)
        private String reason;

        private CompensationStatus status;

        @Future
        private Instant expiredAt;

        private JsonNode metadataJson;

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public CompensationStatus getStatus() {
            return status;
        }

        public void setStatus(CompensationStatus status) {
            this.status = status;
        }

        public Instant getExpiredAt() {
            return expiredAt;
        }

        public void setExpiredAt(Instant expiredAt) {
            this.expiredAt = expiredAt;
        }

        public JsonNode getMetadataJson() {
            return metadataJson;
        }

        public void setMetadataJson(JsonNode metadataJson) {
            this.metadataJson = metadataJson;
        }
    }
}
