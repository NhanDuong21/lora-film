package com.project.analyticsservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "fact_payment_refunds", indexes = {
        @Index(name = "idx_fact_refund_date", columnList = "refund_date"),
        @Index(name = "idx_fact_refund_booking", columnList = "booking_public_id")
})
@Getter
@Setter
@NoArgsConstructor
public class FactPaymentRefund {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "event_id", nullable = false, unique = true, length = 150)
    private String eventId;
    @Column(name = "payment_public_id", nullable = false, length = 64)
    private String paymentPublicId;
    @Column(name = "booking_public_id", nullable = false, length = 64)
    private String bookingPublicId;
    @Column(name = "refund_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal refundAmount;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
    @Column(name = "refund_date", nullable = false)
    private LocalDate refundDate;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
