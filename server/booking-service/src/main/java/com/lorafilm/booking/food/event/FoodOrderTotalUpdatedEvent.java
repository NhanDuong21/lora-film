package com.lorafilm.booking.food.event;

import java.math.BigDecimal;

public class FoodOrderTotalUpdatedEvent {

    private String bookingPublicId;
    private String foodOrderPublicId;
    private BigDecimal finalAmount;

    public FoodOrderTotalUpdatedEvent() {}

    public FoodOrderTotalUpdatedEvent(String bookingPublicId, String foodOrderPublicId, BigDecimal finalAmount) {
        this.bookingPublicId = bookingPublicId;
        this.foodOrderPublicId = foodOrderPublicId;
        this.finalAmount = finalAmount;
    }

    public String getBookingPublicId() {
        return bookingPublicId;
    }

    public void setBookingPublicId(String bookingPublicId) {
        this.bookingPublicId = bookingPublicId;
    }

    public String getFoodOrderPublicId() {
        return foodOrderPublicId;
    }

    public void setFoodOrderPublicId(String foodOrderPublicId) {
        this.foodOrderPublicId = foodOrderPublicId;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount;
    }
}
