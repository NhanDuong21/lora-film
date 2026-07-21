package com.lorafilm.booking.booking.entity;

import com.lorafilm.booking.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "booking_price_snapshots")
public class BookingPriceSnapshot extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "currency", length = 10, nullable = false)
    private String currency = "VND";

    @Column(name = "pricing_engine_version", length = 20)
    private String pricingEngineVersion = "v1.0";

    @Column(name = "pricing_breakdown_json", columnDefinition = "JSON")
    private String pricingBreakdownJson;

    public BookingPriceSnapshot() {
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPricingEngineVersion() {
        return pricingEngineVersion;
    }

    public void setPricingEngineVersion(String pricingEngineVersion) {
        this.pricingEngineVersion = pricingEngineVersion;
    }

    public String getPricingBreakdownJson() {
        return pricingBreakdownJson;
    }

    public void setPricingBreakdownJson(String pricingBreakdownJson) {
        this.pricingBreakdownJson = pricingBreakdownJson;
    }
}
