package com.lorafilm.movie.pricing.domain.entity;

import com.lorafilm.movie.pricing.domain.enums.PricingSource;
import com.lorafilm.movie.seat.domain.entity.SeatType;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "showtime_prices", uniqueConstraints = {@UniqueConstraint(columnNames = {"showtime_id", "seat_type_id"})})
public class ShowtimePrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_type_id", nullable = false)
    private SeatType seatType;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "currency", nullable = false, columnDefinition = "CHAR(3)")
    private String currency = "VND";

    @Column(name = "seat_type_name_snapshot", nullable = false, length = 80)
    private String seatTypeNameSnapshot;

    @Column(name = "seat_type_code_snapshot", nullable = false, length = 30)
    private String seatTypeCodeSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_source", nullable = false, length = 30)
    private PricingSource pricingSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_policy_id")
    private PricePolicy sourcePolicy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_rule_id")
    private PricePolicyRule sourceRule;

    @Column(name = "resolved_at", nullable = false)
    private Instant resolvedAt;

    @Column(name = "resolution_timezone", nullable = false, length = 80)
    private String resolutionTimezone;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private Long updatedBy;

    public ShowtimePrice() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Showtime getShowtime() {
        return showtime;
    }

    public void setShowtime(Showtime showtime) {
        this.showtime = showtime;
    }

    public SeatType getSeatType() {
        return seatType;
    }

    public void setSeatType(SeatType seatType) {
        this.seatType = seatType;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getSeatTypeNameSnapshot() { return seatTypeNameSnapshot; }
    public void setSeatTypeNameSnapshot(String seatTypeNameSnapshot) { this.seatTypeNameSnapshot = seatTypeNameSnapshot; }
    public String getSeatTypeCodeSnapshot() { return seatTypeCodeSnapshot; }
    public void setSeatTypeCodeSnapshot(String seatTypeCodeSnapshot) { this.seatTypeCodeSnapshot = seatTypeCodeSnapshot; }
    public PricingSource getPricingSource() { return pricingSource; }
    public void setPricingSource(PricingSource pricingSource) { this.pricingSource = pricingSource; }
    public PricePolicy getSourcePolicy() { return sourcePolicy; }
    public void setSourcePolicy(PricePolicy sourcePolicy) { this.sourcePolicy = sourcePolicy; }
    public PricePolicyRule getSourceRule() { return sourceRule; }
    public void setSourceRule(PricePolicyRule sourceRule) { this.sourceRule = sourceRule; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
    public String getResolutionTimezone() { return resolutionTimezone; }
    public void setResolutionTimezone(String resolutionTimezone) { this.resolutionTimezone = resolutionTimezone; }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

}
