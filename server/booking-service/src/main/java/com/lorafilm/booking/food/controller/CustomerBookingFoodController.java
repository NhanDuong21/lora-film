package com.lorafilm.booking.food.controller;

import com.lorafilm.booking.common.response.ApiResponse;
import com.lorafilm.booking.food.dto.request.AddFoodItemRequest;
import com.lorafilm.booking.food.dto.request.UpdateFoodQuantityRequest;
import com.lorafilm.booking.food.dto.response.FoodOrderResponse;
import com.lorafilm.booking.food.service.FoodBookingFacadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings/{bookingId}/foods")
@Validated
@Tag(name = "Customer Booking Food API", description = "Manage food and beverage for a booking")
@SecurityRequirement(name = "bearerAuth")
public class CustomerBookingFoodController {

    private final FoodBookingFacadeService foodBookingFacadeService;

    public CustomerBookingFoodController(FoodBookingFacadeService foodBookingFacadeService) {
        this.foodBookingFacadeService = foodBookingFacadeService;
    }

    @GetMapping
    @Operation(summary = "Get food order", description = "View the food order for a booking")
    public ResponseEntity<ApiResponse<FoodOrderResponse>> getFoodOrder(@PathVariable String bookingId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Food order retrieved successfully", foodBookingFacadeService.getFoodOrder(bookingId)));
    }

    @PostMapping
    @Operation(summary = "Add food item", description = "Add a new food item to the booking")
    public ResponseEntity<ApiResponse<FoodOrderResponse>> addFoodItem(
            @PathVariable String bookingId,
            @Valid @RequestBody AddFoodItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Food item added successfully", foodBookingFacadeService.addFoodItem(bookingId, request)));
    }

    @PutMapping("/{foodItemId}")
    @Operation(summary = "Update food quantity", description = "Update the quantity of an existing food item")
    public ResponseEntity<ApiResponse<FoodOrderResponse>> updateFoodQuantity(
            @PathVariable String bookingId,
            @PathVariable Long foodItemId,
            @Valid @RequestBody UpdateFoodQuantityRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Food item updated successfully", foodBookingFacadeService.updateFoodQuantity(bookingId, foodItemId, request)));
    }

    @DeleteMapping("/{foodItemId}")
    @Operation(summary = "Remove food item", description = "Remove a food item from the booking")
    public ResponseEntity<ApiResponse<Void>> removeFoodItem(
            @PathVariable String bookingId,
            @PathVariable Long foodItemId) {
        foodBookingFacadeService.removeFoodItem(bookingId, foodItemId);
        return ResponseEntity.ok(ApiResponse.success("Food item removed successfully", null));
    }
}
