package com.project.paymentservice.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.project.paymentservice.common.MoneyJsonSerializer;
import com.project.paymentservice.entity.PaymentRefund;

import java.math.BigDecimal;
import java.time.Instant;

public class RefundResponse {
    private String refundPublicId;
    private String refundCode;
    private String paymentPublicId;
    private String bookingPublicId;
    private String provider;
    private String refundType;
    private String refundComponent;
    private String reasonCode;
    private String reasonDetail;
    @JsonSerialize(using = MoneyJsonSerializer.class)
    private BigDecimal amount;
    private String currency;
    private boolean automatic;
    private String status;
    private String providerRefundId;
    private String failureCode;
    private String failureMessage;
    private Instant requestedAt;
    private Instant succeededAt;
    private Instant failedAt;

    public static RefundResponse from(PaymentRefund refund) {
        RefundResponse response = new RefundResponse();
        response.refundPublicId = refund.getPublicId();
        response.refundCode = refund.getRefundCode();
        response.paymentPublicId = refund.getPayment().getPublicId();
        response.bookingPublicId = refund.getPayment().getBookingPublicId();
        response.provider = refund.getProviderCode().name();
        response.refundType = refund.getRefundType().name();
        response.refundComponent = refund.getRefundComponent().name();
        response.reasonCode = refund.getReasonCode();
        response.reasonDetail = refund.getReasonDetailSanitized();
        response.amount = refund.getRequestedAmount();
        response.currency = refund.getCurrency();
        response.automatic = refund.isAutomatic();
        response.status = refund.getStatus().name();
        response.providerRefundId = refund.getProviderRefundId();
        response.failureCode = refund.getFailureCode();
        response.failureMessage = refund.getFailureMessageSanitized();
        response.requestedAt = refund.getRequestedAt();
        response.succeededAt = refund.getSucceededAt();
        response.failedAt = refund.getFailedAt();
        return response;
    }

    public String getRefundPublicId() { return refundPublicId; }
    public String getRefundCode() { return refundCode; }
    public String getPaymentPublicId() { return paymentPublicId; }
    public String getBookingPublicId() { return bookingPublicId; }
    public String getProvider() { return provider; }
    public String getRefundType() { return refundType; }
    public String getRefundComponent() { return refundComponent; }
    public String getReasonCode() { return reasonCode; }
    public String getReasonDetail() { return reasonDetail; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public boolean isAutomatic() { return automatic; }
    public String getStatus() { return status; }
    public String getProviderRefundId() { return providerRefundId; }
    public String getFailureCode() { return failureCode; }
    public String getFailureMessage() { return failureMessage; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getSucceededAt() { return succeededAt; }
    public Instant getFailedAt() { return failedAt; }
}
