package com.lorafilm.booking.food.client;

import com.lorafilm.booking.food.enums.ProductType;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MockFoodCatalogClientImpl implements FoodCatalogClient {

    private final Map<Long, FoodCatalogItem> catalog = new ConcurrentHashMap<>();

    public MockFoodCatalogClientImpl() {
        catalog.put(101L, new FoodCatalogItem(101L, "POP_L", "Large Popcorn", ProductType.FOOD, "popcorn_l.png", new BigDecimal("50000.00"), true, true, false, false, "VND"));
        catalog.put(102L, new FoodCatalogItem(102L, "COKE_L", "Large Coke", ProductType.DRINK, "coke_l.png", new BigDecimal("30000.00"), true, true, false, false, "VND"));
        catalog.put(103L, new FoodCatalogItem(103L, "COMBO_1", "Combo 1 (1 Popcorn, 1 Coke)", ProductType.COMBO, "combo_1.png", new BigDecimal("75000.00"), true, true, false, false, "VND"));
    }

    @Override
    @Cacheable(value = "food_catalog", key = "#productId")
    public Optional<FoodCatalogItem> getProductById(Long productId) {
        return Optional.ofNullable(catalog.get(productId));
    }
}
