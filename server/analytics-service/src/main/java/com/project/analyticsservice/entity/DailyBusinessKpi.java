package com.project.analyticsservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "daily_business_kpis")
@Getter
@Setter
@NoArgsConstructor
public class DailyBusinessKpi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "stat_date", nullable = false, unique = true)
    private LocalDate statDate;
    @Column(name = "gross_revenue", nullable = false, precision = 19, scale = 2)
    private BigDecimal grossRevenue;
    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount;
    @Column(name = "refund_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal refundAmount;
    @Column(name = "net_revenue", nullable = false, precision = 19, scale = 2)
    private BigDecimal netRevenue;
    @Column(name = "booking_count", nullable = false)
    private Long bookingCount;
    @Column(name = "refund_booking_count", nullable = false)
    private Long refundBookingCount;
    @Column(name = "cancelled_booking_count", nullable = false)
    private Long cancelledBookingCount;
    @Column(name = "ticket_count", nullable = false)
    private Long ticketCount;
    @Column(name = "new_customer_count", nullable = false)
    private Long newCustomerCount;
    @Column(name = "returning_customer_count", nullable = false)
    private Long returningCustomerCount;
    @Column(name = "average_booking_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal averageBookingValue;
    @Column(name = "average_ticket_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal averageTicketPrice;
    @Column(name = "refund_rate", nullable = false, precision = 12, scale = 6)
    private BigDecimal refundRate;
    @Column(name = "cancel_rate", nullable = false, precision = 12, scale = 6)
    private BigDecimal cancelRate;
    @Column(name = "promotion_usage_rate", nullable = false, precision = 12, scale = 6)
    private BigDecimal promotionUsageRate;
    @Column(name = "occupancy_rate", nullable = false, precision = 12, scale = 6)
    private BigDecimal occupancyRate;
    @Column(name = "data_completeness", nullable = false, precision = 12, scale = 6)
    private BigDecimal dataCompleteness;
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private Long version;
}
