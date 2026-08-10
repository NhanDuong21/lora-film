package com.lorafilm.booking.food.repository;

import com.lorafilm.booking.food.client.FoodCatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FoodCatalogItemRepository extends JpaRepository<FoodCatalogItem, Long> {
    Optional<FoodCatalogItem> findByCode(String code);

    Optional<FoodCatalogItem> findByCodeIgnoreCase(String code);
}
