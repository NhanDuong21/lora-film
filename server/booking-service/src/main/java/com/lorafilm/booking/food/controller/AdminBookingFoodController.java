package com.lorafilm.booking.food.controller;

import com.lorafilm.booking.common.response.ApiResponse;
import com.lorafilm.booking.food.client.FoodCatalogClient;
import com.lorafilm.booking.food.client.FoodCatalogItem;
import com.lorafilm.booking.food.dto.response.FoodItemSalesDto;
import com.lorafilm.booking.food.dto.response.FoodOrderResponse;
import com.lorafilm.booking.food.repository.FoodOrderItemRepository;
import com.lorafilm.booking.food.service.CloudinaryService;
import com.lorafilm.booking.food.service.FoodBookingFacadeService;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin Booking Food API", description = "Admin endpoints for managing booking foods")
@SecurityRequirement(name = "bearerAuth")
public class AdminBookingFoodController {

    private final FoodBookingFacadeService foodBookingFacadeService;
    private final FoodCatalogClient foodCatalogClient;
    private final FoodOrderItemRepository foodOrderItemRepository;
    private final CloudinaryService cloudinaryService;

    public AdminBookingFoodController(FoodBookingFacadeService foodBookingFacadeService,
                                     FoodCatalogClient foodCatalogClient,
                                     FoodOrderItemRepository foodOrderItemRepository,
                                     CloudinaryService cloudinaryService) {
        this.foodBookingFacadeService = foodBookingFacadeService;
        this.foodCatalogClient = foodCatalogClient;
        this.foodOrderItemRepository = foodOrderItemRepository;
        this.cloudinaryService = cloudinaryService;
    }

    @GetMapping("/bookings/{bookingId}/foods")
    @Operation(summary = "Get food order for booking", description = "Admin can view any booking's food order")
    public ResponseEntity<ApiResponse<FoodOrderResponse>> getFoodOrder(@PathVariable String bookingId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Food order retrieved successfully", foodBookingFacadeService.getFoodOrder(bookingId)));
    }

    @GetMapping("/foods")
    @Operation(summary = "Get all food items", description = "Admin can view all food products in catalog")
    public ResponseEntity<ApiResponse<List<FoodCatalogItem>>> getAllFoods() {
        return ResponseEntity.ok(ApiResponse.success("Food catalog retrieved successfully", foodCatalogClient.getAllProducts()));
    }

    @PostMapping(value = "/foods", consumes = {"multipart/form-data"})
    @Operation(summary = "Add a new food item", description = "Admin can create a new food product")
    public ResponseEntity<ApiResponse<FoodCatalogItem>> addFood(
            @RequestPart("item") FoodCatalogItem item,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        if (image != null && !image.isEmpty()) {
            var mediaResponse = cloudinaryService.uploadImage(image, "foods", item.getCode());
            item.setImageUrl(mediaResponse.getSecureUrl());
        }
        FoodCatalogItem created = foodCatalogClient.addProduct(item);
        return ResponseEntity.ok(ApiResponse.success("Food item created successfully", created));
    }

    @PutMapping(value = "/foods/{id}", consumes = {"multipart/form-data"})
    @Operation(summary = "Update a food item", description = "Admin can update details of a food product")
    public ResponseEntity<ApiResponse<FoodCatalogItem>> updateFood(
            @PathVariable Long id, 
            @RequestPart("item") FoodCatalogItem item,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        if (image != null && !image.isEmpty()) {
            var mediaResponse = cloudinaryService.uploadImage(image, "foods", item.getCode());
            item.setImageUrl(mediaResponse.getSecureUrl());
        }
        FoodCatalogItem updated = foodCatalogClient.updateProduct(id, item);
        if (updated != null) {
            return ResponseEntity.ok(ApiResponse.success("Food item updated successfully", updated));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/foods/{id}")
    @Operation(summary = "Delete a food item", description = "Admin can delete/disable a food product")
    public ResponseEntity<ApiResponse<Void>> deleteFood(@PathVariable Long id) {
        boolean deleted = foodCatalogClient.deleteProduct(id);
        if (deleted) {
            return ResponseEntity.ok(ApiResponse.success("Food item deleted successfully", null));
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/foods/statistics")
    @Operation(summary = "Get food statistics", description = "Admin can view food order statistics")
    public ResponseEntity<ApiResponse<List<FoodItemSalesDto>>> getFoodStatistics() {
        return ResponseEntity.ok(ApiResponse.success(
                "Food statistics retrieved successfully", foodOrderItemRepository.getFoodSalesStatistics()));
    }
}
