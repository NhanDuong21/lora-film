package com.lorafilm.booking.food.client;

import java.util.List;
import java.util.Optional;

public interface FoodCatalogClient {
    Optional<FoodCatalogItem> getProductById(Long productId);
    List<FoodCatalogItem> getAllProducts();
    FoodCatalogItem addProduct(FoodCatalogItem item);
    FoodCatalogItem updateProduct(Long id, FoodCatalogItem updated);
    boolean deleteProduct(Long id);
}
