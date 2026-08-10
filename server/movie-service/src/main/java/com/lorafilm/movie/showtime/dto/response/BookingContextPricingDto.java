package com.lorafilm.movie.showtime.dto.response;

import java.math.BigDecimal;

public class BookingContextPricingDto {
    private BigDecimal seatAmount;
    private BigDecimal discountAmount;
    private BigDecimal serviceFee;
    private BigDecimal totalAmount;
    private String currency;

    public BookingContextPricingDto() {
        this.discountAmount = BigDecimal.ZERO;
        this.serviceFee = BigDecimal.ZERO;
    }

    public BigDecimal getSeatAmount() {
        return seatAmount;
    }

    public void setSeatAmount(BigDecimal seatAmount) {
        this.seatAmount = seatAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getServiceFee() {
        return serviceFee;
    }

    public void setServiceFee(BigDecimal serviceFee) {
        this.serviceFee = serviceFee;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
