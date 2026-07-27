package com.lorafilm.booking.food.client;

import com.lorafilm.booking.common.exception.BusinessException;
import com.lorafilm.booking.food.repository.FoodCatalogItemRepository;
import org.springframework.http.HttpStatus;
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
        normalize(item);
        if (foodCatalogItemRepository.findByCodeIgnoreCase(item.getCode()).isPresent()) {
            throw new BusinessException(
                    "CONCESSION_CODE_EXISTS",
                    "A concession product with this code already exists",
                    HttpStatus.CONFLICT);
        }
        if (item.getCurrency() == null) {
            item.setCurrency("VND");
        }
        item.setDeleted(false);
        item.setDisabled(false);
        return foodCatalogItemRepository.save(item);
    }

    @Override
    @Transactional
    public FoodCatalogItem updateProduct(Long id, FoodCatalogItem updated) {
        normalize(updated);
        return foodCatalogItemRepository.findById(id)
                .map(existing -> {
                    if (existing.isDeleted()) {
                        throw new BusinessException(
                                "CONCESSION_ARCHIVED",
                                "Archived concession products must be restored before editing",
                                HttpStatus.CONFLICT);
                    }
                    if (!existing.getCode().equalsIgnoreCase(updated.getCode())) {
                        throw new BusinessException(
                                "CONCESSION_CODE_IMMUTABLE",
                                "Product code cannot be changed after creation",
                                HttpStatus.CONFLICT);
                    }
                    existing.setName(updated.getName());
                    existing.setType(updated.getType());
                    existing.setImageUrl(updated.getImageUrl());
                    existing.setPrice(updated.getPrice());
                    existing.setActive(updated.isActive());
                    existing.setSellable(updated.isSellable());
                    existing.setCurrency("VND");
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

    @Override
    @Transactional
    public FoodCatalogItem restoreProduct(Long id) {
        return foodCatalogItemRepository.findById(id)
                .map(existing -> {
                    if (!existing.isDeleted()) {
                        return existing;
                    }
                    existing.setDeleted(false);
                    existing.setDisabled(false);
                    existing.setActive(false);
                    existing.setSellable(false);
                    return foodCatalogItemRepository.save(existing);
                })
                .orElse(null);
    }

    private void normalize(FoodCatalogItem item) {
        item.setCode(item.getCode().trim().toUpperCase());
        item.setName(item.getName().trim());
        item.setImageUrl(item.getImageUrl() == null || item.getImageUrl().isBlank()
                ? null
                : item.getImageUrl().trim());
    }
}
