package com.lorafilm.booking.food.dto.request;

import com.lorafilm.booking.food.enums.ProductType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AdminFoodCatalogItemRequest(
        @NotBlank(message = "Product code is required")
        @Size(max = 50, message = "Product code must not exceed 50 characters")
        @Pattern(
                regexp = "^[A-Za-z0-9_-]+$",
                message = "Product code may only contain letters, numbers, underscores and hyphens")
        String code,

        @NotBlank(message = "Product name is required")
        @Size(max = 255, message = "Product name must not exceed 255 characters")
        String name,

        @NotNull(message = "Product type is required")
        ProductType type,

        @Size(max = 500, message = "Image URL must not exceed 500 characters")
        String imageUrl,

        @NotNull(message = "Product price is required")
        @DecimalMin(value = "1000", message = "Product price must be at least 1,000 VND")
        BigDecimal price,

        @NotNull(message = "Active status is required")
        Boolean active,

        @NotNull(message = "Sellable status is required")
        Boolean sellable
) {
}
