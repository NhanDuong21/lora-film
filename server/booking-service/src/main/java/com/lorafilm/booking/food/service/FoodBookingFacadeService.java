package com.lorafilm.booking.food.service;

import com.lorafilm.booking.food.dto.request.AddFoodItemRequest;
import com.lorafilm.booking.food.dto.request.UpdateFoodQuantityRequest;
import com.lorafilm.booking.food.dto.response.FoodOrderResponse;

public interface FoodBookingFacadeService {
    FoodOrderResponse getFoodOrder(String bookingPublicId);
    FoodOrderResponse addFoodItem(String bookingPublicId, AddFoodItemRequest request);
    FoodOrderResponse updateFoodQuantity(String bookingPublicId, Long foodItemId, UpdateFoodQuantityRequest request);
    void removeFoodItem(String bookingPublicId, Long foodItemId);
}
