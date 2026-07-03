package com.project.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_payment_guards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingPaymentGuard {

    @Id
    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "active_payment_id")
    private Long activePaymentId;

    @Column(name = "successful_payment_id")
    private Long successfulPaymentId;

    @Column(name = "next_attempt_number", nullable = false)
    @Builder.Default
    private Integer nextAttemptNumber = 1;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
