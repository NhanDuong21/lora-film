package com.lorafilm.movie.pricing.dto.response;

import java.math.BigDecimal;

public class ShowtimePriceDto {
    private String seatTypeId;
    private String seatTypeName;
    private String seatTypeCode;
    private BigDecimal price;

    public ShowtimePriceDto() {}

    public ShowtimePriceDto(String seatTypeId, BigDecimal price) {
        this.seatTypeId = seatTypeId;
        this.price = price;
    }

    public ShowtimePriceDto(String seatTypeId, String seatTypeName, String seatTypeCode, BigDecimal price) {
        this.seatTypeId = seatTypeId;
        this.seatTypeName = seatTypeName;
        this.seatTypeCode = seatTypeCode;
        this.price = price;
    }

    public String getSeatTypeId() {
        return seatTypeId;
    }

    public void setSeatTypeId(String seatTypeId) {
        this.seatTypeId = seatTypeId;
    }

    public String getSeatTypeName() {
        return seatTypeName;
    }

    public void setSeatTypeName(String seatTypeName) {
        this.seatTypeName = seatTypeName;
    }

    public String getSeatTypeCode() {
        return seatTypeCode;
    }

    public void setSeatTypeCode(String seatTypeCode) {
        this.seatTypeCode = seatTypeCode;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
