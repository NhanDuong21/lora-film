package com.lorafilm.booking.food.service;

import com.lorafilm.booking.food.dto.request.AddFoodItemRequest;
import com.lorafilm.booking.food.dto.request.UpdateFoodQuantityRequest;
import com.lorafilm.booking.food.dto.response.FoodOrderResponse;

public interface FoodOrderService {
    
    FoodOrderResponse getFoodOrder(String publicId);
    
    FoodOrderResponse getFoodOrderByBookingId(Long bookingId);

    FoodOrderResponse createFoodOrder(Long bookingId);

    FoodOrderResponse addFoodItem(String foodOrderPublicId, AddFoodItemRequest request);

    FoodOrderResponse updateFoodQuantity(String foodOrderPublicId, Long itemId, UpdateFoodQuantityRequest request);

    void removeFoodItem(String foodOrderPublicId, Long itemId);

    FoodOrderResponse createOrGetFoodOrder(Long bookingId);

    void updateOrderStatusBasedOnBooking(Long bookingId, com.lorafilm.booking.booking.enums.BookingStatus bookingStatus);
}
