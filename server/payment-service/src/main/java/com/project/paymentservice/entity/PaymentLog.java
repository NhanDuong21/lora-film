package com.project.paymentservice.entity;

import com.project.paymentservice.enumtype.ActorType;
import com.project.paymentservice.enumtype.PaymentLogEventType;
import com.project.paymentservice.enumtype.PaymentStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_logs")
public class PaymentLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private PaymentLogEventType eventType;

    @Column(name = "source", nullable = false, length = 50)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 30)
    private ActorType actorType;

    @Column(name = "actor_account_id")
    private Long actorAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 30)
    private PaymentStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false, length = 30)
    private PaymentStatus currentStatus;

    @Column(name = "message_sanitized", columnDefinition = "TEXT")
    private String messageSanitized;

    @Column(name = "metadata_sanitized", columnDefinition = "TEXT")
    private String metadataSanitized;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public PaymentLog() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public PaymentLogEventType getEventType() {
        return eventType;
    }

    public void setEventType(PaymentLogEventType eventType) {
        this.eventType = eventType;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public ActorType getActorType() {
        return actorType;
    }

    public void setActorType(ActorType actorType) {
        this.actorType = actorType;
    }

    public Long getActorAccountId() {
        return actorAccountId;
    }

    public void setActorAccountId(Long actorAccountId) {
        this.actorAccountId = actorAccountId;
    }

    public PaymentStatus getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(PaymentStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    public PaymentStatus getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(PaymentStatus currentStatus) {
        this.currentStatus = currentStatus;
    }

    public String getMessageSanitized() {
        return messageSanitized;
    }

    public void setMessageSanitized(String messageSanitized) {
        this.messageSanitized = messageSanitized;
    }

    public String getMetadataSanitized() {
        return metadataSanitized;
    }

    public void setMetadataSanitized(String metadataSanitized) {
        this.metadataSanitized = metadataSanitized;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
