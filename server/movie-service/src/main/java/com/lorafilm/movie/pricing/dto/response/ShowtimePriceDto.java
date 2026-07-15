package com.lorafilm.movie.pricing.dto.response;

import java.math.BigDecimal;

public class ShowtimePriceDto {
    private String seatTypeId;
    private BigDecimal price;

    public ShowtimePriceDto() {}

    public ShowtimePriceDto(String seatTypeId, BigDecimal price) {
        this.seatTypeId = seatTypeId;
        this.price = price;
    }

    public String getSeatTypeId() {
        return seatTypeId;
    }

    public void setSeatTypeId(String seatTypeId) {
        this.seatTypeId = seatTypeId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
