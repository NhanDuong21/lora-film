package com.lorafilm.booking.food.service;

import com.lorafilm.booking.food.dto.request.AddFoodItemRequest;
import com.lorafilm.booking.food.dto.request.UpdateFoodQuantityRequest;
import com.lorafilm.booking.food.dto.response.FoodItemResponse;
import com.lorafilm.booking.food.dto.response.FoodOrderResponse;

public interface BookingFoodItemService {

    FoodOrderResponse addFoodItem(String bookingId, AddFoodItemRequest request);

    FoodOrderResponse updateQuantity(String bookingId, Long foodItemId, UpdateFoodQuantityRequest request);

    FoodOrderResponse removeFoodItem(String bookingId, Long foodItemId);
}
