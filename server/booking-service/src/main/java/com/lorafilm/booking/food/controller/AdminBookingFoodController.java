package com.lorafilm.booking.food.controller;

import com.lorafilm.booking.common.response.ApiResponse;
import com.lorafilm.booking.food.dto.response.FoodOrderResponse;
import com.lorafilm.booking.food.service.FoodBookingFacadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin Booking Food API", description = "Admin endpoints for managing booking foods")
@SecurityRequirement(name = "bearerAuth")
public class AdminBookingFoodController {

    private final FoodBookingFacadeService foodBookingFacadeService;

    public AdminBookingFoodController(FoodBookingFacadeService foodBookingFacadeService) {
        this.foodBookingFacadeService = foodBookingFacadeService;
    }

    @GetMapping("/bookings/{bookingId}/foods")
    @Operation(summary = "Get food order for booking", description = "Admin can view any booking's food order")
    public ResponseEntity<ApiResponse<FoodOrderResponse>> getFoodOrder(@PathVariable String bookingId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Food order retrieved successfully", foodBookingFacadeService.getFoodOrder(bookingId)));
    }

    @GetMapping("/foods/statistics")
    @Operation(summary = "Get food statistics", description = "Admin can view food order statistics")
    public ResponseEntity<ApiResponse<Object>> getFoodStatistics() {
        return ResponseEntity.ok(ApiResponse.success(
                "Food statistics retrieved successfully", "dummy_statistics_not_implemented"));
    }
}
