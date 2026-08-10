package com.lorafilm.booking.booking.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record BookingFoodResponse(
        Integer totalQuantity,
        BigDecimal totalAmount,
        List<Item> items) {

    public record Item(
            String name,
            String imageUrl,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal totalAmount) {
    }
}
