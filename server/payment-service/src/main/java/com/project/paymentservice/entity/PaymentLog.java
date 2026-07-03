package com.project.paymentservice.entity;

import com.project.paymentservice.enumtype.ActorType;
import com.project.paymentservice.enumtype.PaymentLogEventType;
import com.project.paymentservice.enumtype.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
}
