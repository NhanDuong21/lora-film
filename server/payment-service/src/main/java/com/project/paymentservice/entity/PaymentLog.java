package com.project.paymentservice.entity;

import com.project.paymentservice.enumtype.ActorType;
import com.project.paymentservice.enumtype.PaymentLogEventType;
import com.project.paymentservice.enumtype.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

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
    @Column(name = "message_sanitized", columnDefinition = "text")
    private String messageSanitized;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_sanitized", columnDefinition = "json")
    private String metadataSanitized;
    @Column(name = "correlation_id", length = 100)
    private String correlationId;
    @Column(name = "trace_id", length = 100)
    private String traceId;
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    public PaymentLog() {
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    public PaymentLogEventType getEventType() { return eventType; }
    public void setEventType(PaymentLogEventType eventType) { this.eventType = eventType; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public ActorType getActorType() { return actorType; }
    public void setActorType(ActorType actorType) { this.actorType = actorType; }
    public Long getActorAccountId() { return actorAccountId; }
    public void setActorAccountId(Long value) { this.actorAccountId = value; }
    public PaymentStatus getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(PaymentStatus value) { this.previousStatus = value; }
    public PaymentStatus getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(PaymentStatus value) { this.currentStatus = value; }
    public String getMessageSanitized() { return messageSanitized; }
    public void setMessageSanitized(String value) { this.messageSanitized = value; }
    public String getMetadataSanitized() { return metadataSanitized; }
    public void setMetadataSanitized(String value) { this.metadataSanitized = value; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
