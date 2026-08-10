package com.lorafilm.booking.food.dto.response;

import com.lorafilm.booking.food.enums.ProductType;
import java.math.BigDecimal;

public record FoodItemSalesDto(
        String productName,
        String productCode,
        ProductType productType,
        Long totalQuantity,
        BigDecimal totalAmount
) {
}
