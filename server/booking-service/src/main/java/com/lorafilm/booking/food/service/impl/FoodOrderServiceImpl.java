package com.lorafilm.booking.food.service.impl;

import com.lorafilm.booking.common.exception.BusinessException;
import com.lorafilm.booking.common.exception.NotFoundException;
import com.lorafilm.booking.food.client.FoodCatalogClient;
import com.lorafilm.booking.food.client.FoodCatalogItem;
import com.lorafilm.booking.food.dto.request.AddFoodItemRequest;
import com.lorafilm.booking.food.dto.request.UpdateFoodQuantityRequest;
import com.lorafilm.booking.food.dto.response.FoodOrderResponse;
import com.lorafilm.booking.food.entity.FoodOrder;
import com.lorafilm.booking.food.entity.FoodOrderItem;
import com.lorafilm.booking.food.enums.FoodOrderStatus;
import com.lorafilm.booking.food.mapper.FoodMapper;
import com.lorafilm.booking.food.repository.FoodOrderItemRepository;
import com.lorafilm.booking.food.repository.FoodOrderRepository;
import com.lorafilm.booking.food.service.FoodOrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

@Service
public class FoodOrderServiceImpl implements FoodOrderService {

    private final FoodOrderRepository foodOrderRepository;
    private final FoodOrderItemRepository foodOrderItemRepository;
    private final FoodCatalogClient foodCatalogClient;

    public FoodOrderServiceImpl(FoodOrderRepository foodOrderRepository,
                                FoodOrderItemRepository foodOrderItemRepository,
                                FoodCatalogClient foodCatalogClient) {
        this.foodOrderRepository = foodOrderRepository;
        this.foodOrderItemRepository = foodOrderItemRepository;
        this.foodCatalogClient = foodCatalogClient;
    }

    @Override
    @Transactional(readOnly = true)
    public FoodOrderResponse getFoodOrder(String publicId) {
        FoodOrder foodOrder = foodOrderRepository.findByPublicId(publicId)
                .orElseThrow(() -> new NotFoundException("FoodOrder", "publicId", publicId));
        return FoodMapper.INSTANCE.toFoodOrderResponse(foodOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public FoodOrderResponse getFoodOrderByBookingId(Long bookingId) {
        return foodOrderRepository.findByBookingId(bookingId)
                .map(FoodMapper.INSTANCE::toFoodOrderResponse)
                .orElse(null);
    }

    @Override
    @Transactional
    public FoodOrderResponse createFoodOrder(Long bookingId) {
        FoodOrder foodOrder = new FoodOrder();
        foodOrder.setPublicId(UUID.randomUUID().toString());
        foodOrder.setBookingId(bookingId);
        foodOrder.setStatus(FoodOrderStatus.PENDING);

        FoodOrder saved = foodOrderRepository.save(foodOrder);
        return FoodMapper.INSTANCE.toFoodOrderResponse(saved);
    }

    @Override
    @Transactional
    public FoodOrderResponse createOrGetFoodOrder(Long bookingId) {
        Optional<FoodOrder> existing = foodOrderRepository.findByBookingId(bookingId);
        if (existing.isPresent()) {
            return FoodMapper.INSTANCE.toFoodOrderResponse(existing.get());
        }
        return createFoodOrder(bookingId);
    }

    @Override
    @Transactional
    public FoodOrderResponse addFoodItem(String foodOrderPublicId, AddFoodItemRequest request) {
        FoodOrder foodOrder = foodOrderRepository.findByPublicId(foodOrderPublicId)
                .orElseThrow(() -> new NotFoundException("FoodOrder", "publicId", foodOrderPublicId));

        validateOrderModifiable(foodOrder);

        FoodCatalogItem catalogItem = foodCatalogClient.getProductById(request.getProductId())
                .orElseThrow(() -> new BusinessException("FOOD_NOT_FOUND", "Product not found"));
        if (!catalogItem.isActive()) {
            throw new BusinessException("FOOD_NOT_AVAILABLE", "Product is not available or inactive");
        }

        Optional<FoodOrderItem> existingItemOpt = foodOrder.getItems().stream()
                .filter(i -> i.getProductId().equals(request.getProductId()))
                .findFirst();

        if (existingItemOpt.isPresent()) {
            FoodOrderItem existingItem = existingItemOpt.get();
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
            calculateItemTotals(existingItem);
        } else {
            FoodOrderItem newItem = new FoodOrderItem();
            newItem.setFoodOrder(foodOrder);
            newItem.setProductId(catalogItem.getId());
            newItem.setProductCode(catalogItem.getCode());
            newItem.setProductName(catalogItem.getName());
            newItem.setProductType(catalogItem.getType());
            newItem.setProductImage(catalogItem.getImageUrl());
            newItem.setQuantity(request.getQuantity());
            newItem.setUnitPrice(catalogItem.getPrice());
            newItem.setDiscountAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

            calculateItemTotals(newItem);
            foodOrder.getItems().add(newItem);
        }

        recalculateOrderTotals(foodOrder);
        FoodOrder saved = foodOrderRepository.save(foodOrder);
        return FoodMapper.INSTANCE.toFoodOrderResponse(saved);
    }

    @Override
    @Transactional
    public FoodOrderResponse updateFoodQuantity(String foodOrderPublicId, Long itemId, UpdateFoodQuantityRequest request) {
        FoodOrder foodOrder = foodOrderRepository.findByPublicId(foodOrderPublicId)
                .orElseThrow(() -> new NotFoundException("FoodOrder", "publicId", foodOrderPublicId));

        validateOrderModifiable(foodOrder);

        FoodOrderItem item = foodOrder.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("FoodOrderItem", "id", itemId.toString()));

        item.setQuantity(request.getQuantity());
        calculateItemTotals(item);

        recalculateOrderTotals(foodOrder);
        FoodOrder saved = foodOrderRepository.save(foodOrder);
        return FoodMapper.INSTANCE.toFoodOrderResponse(saved);
    }

    @Override
    @Transactional
    public void removeFoodItem(String foodOrderPublicId, Long itemId) {
        FoodOrder foodOrder = foodOrderRepository.findByPublicId(foodOrderPublicId)
                .orElseThrow(() -> new NotFoundException("FoodOrder", "publicId", foodOrderPublicId));

        validateOrderModifiable(foodOrder);

        boolean removed = foodOrder.getItems().removeIf(i -> i.getId().equals(itemId));
        if (!removed) {
            throw new NotFoundException("FoodOrderItem", "id", itemId.toString());
        }

        recalculateOrderTotals(foodOrder);
        foodOrderRepository.save(foodOrder);
    }

    @Override
    @Transactional
    public void updateOrderStatusBasedOnBooking(Long bookingId, com.lorafilm.booking.booking.enums.BookingStatus bookingStatus) {
        Optional<FoodOrder> orderOpt = foodOrderRepository.findByBookingId(bookingId);
        if (orderOpt.isEmpty()) {
            return;
        }
        FoodOrder foodOrder = orderOpt.get();

        switch (bookingStatus) {
            case CONFIRMED:
                foodOrder.setStatus(FoodOrderStatus.CONFIRMED);
                break;
            case CANCELLED:
            case EXPIRED:
                foodOrder.setStatus(FoodOrderStatus.CANCELLED);
                break;
            case REFUNDED:
                foodOrder.setStatus(FoodOrderStatus.REFUNDED);
                break;
            default:
                // No action needed for PENDING_PAYMENT
                break;
        }
        
        foodOrderRepository.save(foodOrder);
    }

    private void calculateItemTotals(FoodOrderItem item) {
        BigDecimal qty = new BigDecimal(item.getQuantity());
        item.setSubtotal(item.getUnitPrice().multiply(qty).setScale(2, RoundingMode.HALF_UP));
        item.setFinalAmount(item.getSubtotal().subtract(item.getDiscountAmount()).setScale(2, RoundingMode.HALF_UP));
    }

    private void recalculateOrderTotals(FoodOrder foodOrder) {
        int totalQty = 0;
        BigDecimal subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        for (FoodOrderItem item : foodOrder.getItems()) {
            totalQty += item.getQuantity();
            subtotal = subtotal.add(item.getSubtotal());
            discount = discount.add(item.getDiscountAmount());
        }

        foodOrder.setTotalQuantity(totalQty);
        foodOrder.setSubtotal(subtotal);
        foodOrder.setDiscountAmount(discount);
        foodOrder.setFinalAmount(subtotal.subtract(discount).setScale(2, RoundingMode.HALF_UP));
    }

    private void validateOrderModifiable(FoodOrder foodOrder) {
        if (foodOrder.getStatus() != FoodOrderStatus.PENDING) {
            throw new BusinessException("ORDER_NOT_MODIFIABLE", "Food order cannot be modified at this stage");
        }
    }
}
