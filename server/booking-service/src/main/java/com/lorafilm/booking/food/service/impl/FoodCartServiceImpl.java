package com.lorafilm.booking.food.service.impl;

import com.lorafilm.booking.common.exception.BusinessException;
import com.lorafilm.booking.common.exception.NotFoundException;
import com.lorafilm.booking.food.client.FoodCatalogClient;
import com.lorafilm.booking.food.client.FoodCatalogItem;
import com.lorafilm.booking.food.dto.request.AddFoodItemRequest;
import com.lorafilm.booking.food.dto.request.UpdateFoodQuantityRequest;
import com.lorafilm.booking.food.dto.response.FoodOrderResponse;
import com.lorafilm.booking.food.entity.FoodOrder;
import com.lorafilm.booking.food.enums.FoodOrderStatus;
import com.lorafilm.booking.food.mapper.FoodMapper;
import com.lorafilm.booking.food.repository.FoodOrderRepository;
import com.lorafilm.booking.food.service.FoodCartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class FoodCartServiceImpl implements FoodCartService {

    private final FoodOrderRepository foodOrderRepository;
    private final FoodCatalogClient foodCatalogClient;

    public FoodCartServiceImpl(FoodOrderRepository foodOrderRepository, FoodCatalogClient foodCatalogClient) {
        this.foodOrderRepository = foodOrderRepository;
        this.foodCatalogClient = foodCatalogClient;
    }

    @Override
    @Transactional(readOnly = true)
    public FoodOrderResponse getCart(Long userId) {
        FoodOrder cart = getOrCreateCart(userId);
        return FoodMapper.INSTANCE.toFoodOrderResponse(cart);
    }

    @Override
    @Transactional
    public FoodOrderResponse addFoodToCart(Long userId, AddFoodItemRequest request) {
        FoodCatalogItem catalogItem = foodCatalogClient.getProductById(request.getProductId())
                .orElseThrow(() -> new BusinessException("FOOD_NOT_FOUND", "Product not found"));

        FoodOrder cart = getOrCreateCart(userId);
        cart.addItem(catalogItem, request.getQuantity());
        FoodOrder saved = foodOrderRepository.save(cart);

        return FoodMapper.INSTANCE.toFoodOrderResponse(saved);
    }

    @Override
    @Transactional
    public FoodOrderResponse updateFoodQuantity(Long userId, Long foodItemId, UpdateFoodQuantityRequest request) {
        FoodOrder cart = getOrCreateCart(userId);
        cart.updateItemQuantity(foodItemId, request.getQuantity());
        FoodOrder saved = foodOrderRepository.save(cart);

        return FoodMapper.INSTANCE.toFoodOrderResponse(saved);
    }

    @Override
    @Transactional
    public void removeFoodItem(Long userId, Long foodItemId) {
        FoodOrder cart = getOrCreateCart(userId);
        boolean removed = cart.getItems().removeIf(i -> i.getId().equals(foodItemId));
        if (!removed) {
            throw new NotFoundException("FoodOrderItem", "id", foodItemId.toString());
        }
        cart.recalculateTotals();
        foodOrderRepository.save(cart);
    }

    @Override
    @Transactional
    public FoodOrderResponse checkoutCart(Long userId) {
        FoodOrder cart = getOrCreateCart(userId);
        if (cart.getItems().isEmpty()) {
            throw new BusinessException("CART_EMPTY", "Cart is empty");
        }
        cart.setStatus(FoodOrderStatus.CONFIRMED);
        FoodOrder saved = foodOrderRepository.save(cart);
        // Note: Payment logic should be triggered here or in a separate controller
        return FoodMapper.INSTANCE.toFoodOrderResponse(saved);
    }

    @Override
    @Transactional
    public FoodOrderResponse mockPay(Long userId, boolean success) {
        FoodOrder cart = foodOrderRepository.findByUserIdAndStatusAndBookingIsNull(userId, FoodOrderStatus.CONFIRMED)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "No confirmed order found for payment"));
        
        if (success) {
            cart.setPaymentStatus(com.lorafilm.booking.booking.enums.PaymentStatus.SUCCESS);
            cart.setStatus(FoodOrderStatus.CONFIRMED);
            cart.setPaymentMethodSnapshot("MOCK_PAYMENT");
            cart.setPaymentProvider("mock-payment-service");
            cart.setPaymentReference("MOCK-TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        } else {
            cart.setPaymentStatus(com.lorafilm.booking.booking.enums.PaymentStatus.FAILED);
            cart.setStatus(FoodOrderStatus.CANCELLED);
        }
        
        FoodOrder saved = foodOrderRepository.save(cart);
        return FoodMapper.INSTANCE.toFoodOrderResponse(saved);
    }

    private FoodOrder getOrCreateCart(Long userId) {
        return foodOrderRepository.findByUserIdAndStatusAndBookingIsNull(userId, FoodOrderStatus.PENDING)
                .orElseGet(() -> {
                    FoodOrder newCart = new FoodOrder();
                    newCart.setUserId(userId);
                    newCart.setPublicId(UUID.randomUUID().toString());
                    newCart.setStatus(FoodOrderStatus.PENDING);
                    newCart.setPaymentStatus(com.lorafilm.booking.booking.enums.PaymentStatus.PENDING);
                    return foodOrderRepository.save(newCart);
                });
    }
}
