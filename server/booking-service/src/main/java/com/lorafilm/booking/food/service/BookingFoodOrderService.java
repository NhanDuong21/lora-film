package com.lorafilm.booking.food.service;

import com.lorafilm.booking.food.dto.request.AddFoodItemRequest;
import com.lorafilm.booking.food.dto.request.UpdateFoodQuantityRequest;
import com.lorafilm.booking.food.dto.response.FoodItemResponse;
import com.lorafilm.booking.food.dto.response.FoodOrderResponse;
import java.math.BigDecimal;

public interface BookingFoodOrderService {

    FoodOrderResponse createFoodOrder(String bookingId);

    FoodOrderResponse getFoodOrder(String bookingId);

    void removeFoodOrder(String bookingId);

    BigDecimal calculateFoodAmount(String bookingId);

    // Added for admin statistics
    Object getFoodStatistics();
}
