package com.lorafilm.booking.food.client;

import com.lorafilm.booking.food.repository.FoodCatalogItemRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Primary
public class DbFoodCatalogClientImpl implements FoodCatalogClient {

    private final FoodCatalogItemRepository foodCatalogItemRepository;

    public DbFoodCatalogClientImpl(FoodCatalogItemRepository foodCatalogItemRepository) {
        this.foodCatalogItemRepository = foodCatalogItemRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FoodCatalogItem> getProductById(Long productId) {
        return foodCatalogItemRepository.findById(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoodCatalogItem> getAllProducts() {
        return foodCatalogItemRepository.findAll();
    }

    @Override
    @Transactional
    public FoodCatalogItem addProduct(FoodCatalogItem item) {
        if (item.getCurrency() == null) {
            item.setCurrency("VND");
        }
        return foodCatalogItemRepository.save(item);
    }

    @Override
    @Transactional
    public FoodCatalogItem updateProduct(Long id, FoodCatalogItem updated) {
        return foodCatalogItemRepository.findById(id)
                .map(existing -> {
                    existing.setCode(updated.getCode());
                    existing.setName(updated.getName());
                    existing.setType(updated.getType());
                    existing.setImageUrl(updated.getImageUrl());
                    existing.setPrice(updated.getPrice());
                    existing.setActive(updated.isActive());
                    existing.setSellable(updated.isSellable());
                    existing.setDeleted(updated.isDeleted());
                    existing.setDisabled(updated.isDisabled());
                    if (updated.getCurrency() != null) {
                        existing.setCurrency(updated.getCurrency());
                    }
                    return foodCatalogItemRepository.save(existing);
                })
                .orElse(null);
    }

    @Override
    @Transactional
    public boolean deleteProduct(Long id) {
        return foodCatalogItemRepository.findById(id)
                .map(existing -> {
                    existing.setDeleted(true);
                    existing.setSellable(false);
                    existing.setActive(false);
                    foodCatalogItemRepository.save(existing);
                    return true;
                })
                .orElse(false);
    }
}
