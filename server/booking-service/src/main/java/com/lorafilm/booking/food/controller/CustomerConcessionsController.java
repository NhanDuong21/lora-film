package com.lorafilm.booking.food.controller;

import com.lorafilm.booking.common.response.ApiResponse;
import com.lorafilm.booking.food.client.FoodCatalogClient;
import com.lorafilm.booking.food.client.FoodCatalogItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/customer/concessions")
@Tag(name = "Customer Concessions API", description = "Endpoints for viewing available concessions (popcorn, drinks, combos)")
public class CustomerConcessionsController {

    private final FoodCatalogClient foodCatalogClient;

    public CustomerConcessionsController(FoodCatalogClient foodCatalogClient) {
        this.foodCatalogClient = foodCatalogClient;
    }

    @GetMapping
    @Operation(summary = "Get available concessions catalog", description = "Retrieve list of all sellable and active food and beverage products")
    public ResponseEntity<ApiResponse<List<FoodCatalogItem>>> getConcessions() {
        List<FoodCatalogItem> activeItems = foodCatalogClient.getAllProducts().stream()
                .filter(item -> item.isActive() && item.isSellable() && !item.isDeleted() && !item.isDisabled())
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Concessions catalog retrieved successfully", activeItems));
    }
}
