package com.lorafilm.booking.food.client;

import java.util.Optional;

public interface FoodCatalogClient {
    Optional<FoodCatalogItem> getProductById(Long productId);
}
