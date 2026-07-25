package com.lorafilm.movie.pricing.domain.entity;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.enums.ScreenType;
import com.lorafilm.movie.common.audit.BaseAuditableEntity;
import com.lorafilm.movie.pricing.domain.enums.PriceDayType;
import com.lorafilm.movie.seat.domain.entity.SeatType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "price_policy_rules")
public class PricePolicyRule extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 36,
            columnDefinition = "CHAR(36)")
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private PricePolicy policy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_type_id", nullable = false)
    private SeatType seatType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auditorium_id")
    private Auditorium auditorium;

    @Column(name = "screen_type", length = 30)
    private ScreenType screenType;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_type", nullable = false, length = 20)
    private PriceDayType dayType = PriceDayType.ALL_DAYS;

    @Column(name = "time_band_start")
    private LocalTime timeBandStart;

    @Column(name = "time_band_end")
    private LocalTime timeBandEnd;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public boolean isBoundedTimeBand() {
        return timeBandStart != null && timeBandEnd != null;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public PricePolicy getPolicy() { return policy; }
    public void setPolicy(PricePolicy policy) { this.policy = policy; }
    public SeatType getSeatType() { return seatType; }
    public void setSeatType(SeatType seatType) { this.seatType = seatType; }
    public Auditorium getAuditorium() { return auditorium; }
    public void setAuditorium(Auditorium auditorium) { this.auditorium = auditorium; }
    public ScreenType getScreenType() { return screenType; }
    public void setScreenType(ScreenType screenType) { this.screenType = screenType; }
    public PriceDayType getDayType() { return dayType; }
    public void setDayType(PriceDayType dayType) { this.dayType = dayType; }
    public LocalTime getTimeBandStart() { return timeBandStart; }
    public void setTimeBandStart(LocalTime timeBandStart) { this.timeBandStart = timeBandStart; }
    public LocalTime getTimeBandEnd() { return timeBandEnd; }
    public void setTimeBandEnd(LocalTime timeBandEnd) { this.timeBandEnd = timeBandEnd; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
