package com.lorafilm.booking.payment.entity;

import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.common.entity.BaseEntity;
import com.lorafilm.booking.payment.enums.PaymentEventStatus;
import com.lorafilm.booking.payment.enums.PaymentEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "booking_payment_events")
public class BookingPaymentEvent extends BaseEntity {

    @Column(name = "public_id", length = 36, nullable = false, unique = true)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "payment_public_id", length = 36)
    private String paymentPublicId;

    @Column(name = "schema_version", length = 20, nullable = false)
    private String schemaVersion = "1.0";

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "gateway_transaction_id", length = 100)
    private String gatewayTransactionId;

    @Column(name = "payment_provider", length = 50)
    private String paymentProvider;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private PaymentEventType eventType;

    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 10, nullable = false)
    private String currency = "VND";

    @Column(name = "request_payload", columnDefinition = "JSON")
    private String requestPayload;

    @Column(name = "response_payload", columnDefinition = "JSON")
    private String responsePayload;

    @Column(name = "payload_hash", length = 64)
    private String payloadHash;

    @Column(name = "processing_outcome", length = 40, nullable = false)
    private String processingOutcome = "ACCEPTED";

    @Column(name = "processing_error_code", length = 100)
    private String processingErrorCode;

    @Column(name = "reconciliation_task_public_id", length = 36)
    private String reconciliationTaskPublicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentEventStatus status = PaymentEventStatus.PENDING;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public BookingPaymentEvent() {
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public String getPaymentPublicId() {
        return paymentPublicId;
    }

    public void setPaymentPublicId(String paymentPublicId) {
        this.paymentPublicId = paymentPublicId;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public void setGatewayTransactionId(String gatewayTransactionId) {
        this.gatewayTransactionId = gatewayTransactionId;
    }

    public String getPaymentProvider() {
        return paymentProvider;
    }

    public void setPaymentProvider(String paymentProvider) {
        this.paymentProvider = paymentProvider;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentEventType getEventType() {
        return eventType;
    }

    public void setEventType(PaymentEventType eventType) {
        this.eventType = eventType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getRequestPayload() {
        return requestPayload;
    }

    public void setRequestPayload(String requestPayload) {
        this.requestPayload = requestPayload;
    }

    public String getResponsePayload() {
        return responsePayload;
    }

    public void setResponsePayload(String responsePayload) {
        this.responsePayload = responsePayload;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public void setPayloadHash(String payloadHash) {
        this.payloadHash = payloadHash;
    }

    public String getProcessingOutcome() {
        return processingOutcome;
    }

    public void setProcessingOutcome(String processingOutcome) {
        this.processingOutcome = processingOutcome;
    }

    public String getProcessingErrorCode() {
        return processingErrorCode;
    }

    public void setProcessingErrorCode(String processingErrorCode) {
        this.processingErrorCode = processingErrorCode;
    }

    public String getReconciliationTaskPublicId() {
        return reconciliationTaskPublicId;
    }

    public void setReconciliationTaskPublicId(String reconciliationTaskPublicId) {
        this.reconciliationTaskPublicId = reconciliationTaskPublicId;
    }

    public PaymentEventStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentEventStatus status) {
        this.status = status;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}
