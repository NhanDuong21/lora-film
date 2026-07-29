package com.project.analyticsservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "fact_booking_cancellations", indexes = {
        @Index(name = "idx_fact_cancel_date", columnList = "business_date"),
        @Index(name = "idx_fact_cancel_booking", columnList = "booking_key")
})
@Getter
@Setter
@NoArgsConstructor
public class FactBookingCancellation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "event_id", nullable = false, unique = true, length = 150)
    private String eventId;
    @Column(name = "booking_key", nullable = false, length = 100)
    private String bookingKey;
    @Column(name = "previous_status", nullable = false, length = 50)
    private String previousStatus;
    @Column(name = "reason", nullable = false, length = 50)
    private String reason;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
