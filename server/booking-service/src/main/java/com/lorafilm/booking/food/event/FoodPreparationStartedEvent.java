package com.lorafilm.booking.food.event;

public class FoodPreparationStartedEvent {
    private String foodOrderPublicId;

    public FoodPreparationStartedEvent() {}

    public FoodPreparationStartedEvent(String foodOrderPublicId) {
        this.foodOrderPublicId = foodOrderPublicId;
    }

    public String getFoodOrderPublicId() {
        return foodOrderPublicId;
    }

    public void setFoodOrderPublicId(String foodOrderPublicId) {
        this.foodOrderPublicId = foodOrderPublicId;
    }
}
