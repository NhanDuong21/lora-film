package com.lorafilm.movie.showtime.dto.response;

import java.math.BigDecimal;

public class BookingContextSeatDto {
    private Long seatId;
    private String seatPublicId;
    private String seatCode;
    private String seatType;
    private String pairGroup;
    private BigDecimal price;
    private String currency;

    public BookingContextSeatDto() {}

    public Long getSeatId() {
        return seatId;
    }

    public void setSeatId(Long seatId) {
        this.seatId = seatId;
    }

    public String getSeatPublicId() {
        return seatPublicId;
    }

    public void setSeatPublicId(String seatPublicId) {
        this.seatPublicId = seatPublicId;
    }

    public String getSeatCode() {
        return seatCode;
    }

    public void setSeatCode(String seatCode) {
        this.seatCode = seatCode;
    }

    public String getSeatType() {
        return seatType;
    }

    public void setSeatType(String seatType) {
        this.seatType = seatType;
    }

    public String getPairGroup() {
        return pairGroup;
    }

    public void setPairGroup(String pairGroup) {
        this.pairGroup = pairGroup;
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
}
