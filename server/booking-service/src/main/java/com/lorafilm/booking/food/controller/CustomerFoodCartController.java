package com.lorafilm.booking.food.controller;

import com.lorafilm.booking.common.response.ApiResponse;
import com.lorafilm.booking.food.dto.request.AddFoodItemRequest;
import com.lorafilm.booking.food.dto.request.UpdateFoodQuantityRequest;
import com.lorafilm.booking.food.dto.response.FoodOrderResponse;
import com.lorafilm.booking.food.service.FoodCartService;
import com.lorafilm.booking.security.service.SecurityContextService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/cart")
@Tag(name = "Customer Food Cart API", description = "Endpoints for standalone food cart and checkout")
@SecurityRequirement(name = "bearerAuth")
public class CustomerFoodCartController {

    private final FoodCartService foodCartService;
    private final SecurityContextService securityContextService;

    public CustomerFoodCartController(FoodCartService foodCartService, SecurityContextService securityContextService) {
        this.foodCartService = foodCartService;
        this.securityContextService = securityContextService;
    }

    @GetMapping
    @Operation(summary = "Get current cart", description = "Get current standalone food cart")
    public ResponseEntity<ApiResponse<FoodOrderResponse>> getCart() {
        Long userId = securityContextService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Cart retrieved successfully", foodCartService.getCart(userId)));
    }

    @PostMapping("/items")
    @Operation(summary = "Add food to cart", description = "Add a food item to standalone cart")
    public ResponseEntity<ApiResponse<FoodOrderResponse>> addFoodToCart(@Valid @RequestBody AddFoodItemRequest request) {
        Long userId = securityContextService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Item added to cart", foodCartService.addFoodToCart(userId, request)));
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "Update food quantity", description = "Update quantity of a food item in cart")
    public ResponseEntity<ApiResponse<FoodOrderResponse>> updateFoodQuantity(
            @PathVariable Long itemId, 
            @Valid @RequestBody UpdateFoodQuantityRequest request) {
        Long userId = securityContextService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Quantity updated", foodCartService.updateFoodQuantity(userId, itemId, request)));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove food from cart", description = "Remove a food item from cart")
    public ResponseEntity<ApiResponse<Void>> removeFoodItem(@PathVariable Long itemId) {
        Long userId = securityContextService.getCurrentUserId();
        foodCartService.removeFoodItem(userId, itemId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", null));
    }

    @PostMapping("/checkout")
    @Operation(summary = "Checkout cart", description = "Checkout standalone food cart")
    public ResponseEntity<ApiResponse<FoodOrderResponse>> checkoutCart() {
        Long userId = securityContextService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Cart checked out successfully", foodCartService.checkoutCart(userId)));
    }

    @PostMapping("/mock-pay")
    @Operation(summary = "Mock Pay (Test Only)", description = "Mock the payment process for a confirmed order")
    public ResponseEntity<ApiResponse<FoodOrderResponse>> mockPay(
            @RequestParam(defaultValue = "true") boolean success) {
        try {
            Long userId = securityContextService.getCurrentUserId();
            return ResponseEntity.ok(ApiResponse.success(
                    success ? "Payment mocked as successful" : "Payment mocked as failed",
                    foodCartService.mockPay(userId, success)
            ));
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getClass().getName() + ": " + e.getMessage() + " | Cause: " + (e.getCause() != null ? e.getCause().getMessage() : "none"));
        }
    }
}
