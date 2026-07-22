package com.lorafilm.booking.food.event;

public class FoodOrderPaidEvent {
    private String foodOrderPublicId;
    private String paymentMethod;

    public FoodOrderPaidEvent() {}

    public FoodOrderPaidEvent(String foodOrderPublicId, String paymentMethod) {
        this.foodOrderPublicId = foodOrderPublicId;
        this.paymentMethod = paymentMethod;
    }

    public String getFoodOrderPublicId() {
        return foodOrderPublicId;
    }

    public void setFoodOrderPublicId(String foodOrderPublicId) {
        this.foodOrderPublicId = foodOrderPublicId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
