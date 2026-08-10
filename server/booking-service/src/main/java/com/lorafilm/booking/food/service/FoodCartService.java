package com.lorafilm.booking.food.service;

import com.lorafilm.booking.food.dto.request.AddFoodItemRequest;
import com.lorafilm.booking.food.dto.request.UpdateFoodQuantityRequest;
import com.lorafilm.booking.food.dto.response.FoodOrderResponse;

public interface FoodCartService {
    FoodOrderResponse getCart(Long userId);
    FoodOrderResponse addFoodToCart(Long userId, AddFoodItemRequest request);
    FoodOrderResponse updateFoodQuantity(Long userId, Long foodItemId, UpdateFoodQuantityRequest request);
    void removeFoodItem(Long userId, Long foodItemId);
    FoodOrderResponse checkoutCart(Long userId);
    FoodOrderResponse mockPay(Long userId, boolean success);
}
