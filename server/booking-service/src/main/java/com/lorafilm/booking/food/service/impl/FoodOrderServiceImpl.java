package com.lorafilm.booking.food.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.common.exception.BusinessException;
import com.lorafilm.booking.common.exception.NotFoundException;
import com.lorafilm.booking.food.client.FoodCatalogClient;
import com.lorafilm.booking.food.client.FoodCatalogItem;
import com.lorafilm.booking.food.dto.request.UpdateFoodQuantityRequest;
import com.lorafilm.booking.food.dto.response.FoodOrderResponse;
import com.lorafilm.booking.food.entity.FoodOrder;
import com.lorafilm.booking.food.enums.FoodOrderStatus;
import com.lorafilm.booking.food.event.FoodOrderConfirmedEvent;
import com.lorafilm.booking.food.mapper.FoodMapper;
import com.lorafilm.booking.infrastructure.entity.BookingOutboxEvent;
import com.lorafilm.booking.infrastructure.enums.OutboxStatus;
import com.lorafilm.booking.infrastructure.repository.BookingOutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.lorafilm.booking.food.repository.FoodOrderItemRepository;
import com.lorafilm.booking.food.repository.FoodOrderRepository;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.food.service.FoodOrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

@Service
public class FoodOrderServiceImpl implements FoodOrderService {

    private static final Logger log = LoggerFactory.getLogger(FoodOrderServiceImpl.class);

    private final FoodOrderRepository foodOrderRepository;
    private final FoodOrderItemRepository foodOrderItemRepository;
    private final FoodCatalogClient foodCatalogClient;
    private final BookingOutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final com.lorafilm.booking.infrastructure.monitoring.BookingMetricsManager bookingMetricsManager;
    private final BookingRepository bookingRepository;

    public FoodOrderServiceImpl(FoodOrderRepository foodOrderRepository,
                                FoodOrderItemRepository foodOrderItemRepository,
                                FoodCatalogClient foodCatalogClient,
                                BookingOutboxEventRepository outboxEventRepository,
                                ObjectMapper objectMapper,
                                com.lorafilm.booking.infrastructure.monitoring.BookingMetricsManager bookingMetricsManager,
                                BookingRepository bookingRepository) {
        this.foodOrderRepository = foodOrderRepository;
        this.foodOrderItemRepository = foodOrderItemRepository;
        this.foodCatalogClient = foodCatalogClient;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.bookingMetricsManager = bookingMetricsManager;
        this.bookingRepository = bookingRepository;
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
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException("BOOKING_NOT_FOUND", "Booking not found with ID: " + bookingId));
        foodOrder.setBooking(booking);
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
    public FoodOrderResponse addFoodItem(String foodOrderPublicId, FoodCatalogItem catalogItem, int quantity) {
        FoodOrder foodOrder = foodOrderRepository.findByPublicId(foodOrderPublicId)
                .orElseThrow(() -> new NotFoundException("FoodOrder", "publicId", foodOrderPublicId));

        validateOrderModifiable(foodOrder);

        if (!catalogItem.isActive()) {
            throw new BusinessException("FOOD_NOT_AVAILABLE", "Product is not available or inactive");
        }

        foodOrder.addItem(catalogItem, quantity);

        FoodOrder saved = foodOrderRepository.save(foodOrder);
        return FoodMapper.INSTANCE.toFoodOrderResponse(saved);
    }

    @Override
    @Transactional
    public FoodOrderResponse updateFoodQuantity(String foodOrderPublicId, Long itemId, UpdateFoodQuantityRequest request) {
        FoodOrder foodOrder = foodOrderRepository.findByPublicId(foodOrderPublicId)
                .orElseThrow(() -> new NotFoundException("FoodOrder", "publicId", foodOrderPublicId));

        validateOrderModifiable(foodOrder);
        
        foodOrder.updateItemQuantity(itemId, request.getQuantity());

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

        foodOrder.recalculateTotals();
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
                FoodOrderConfirmedEvent event = new FoodOrderConfirmedEvent(
                        bookingId.toString(),
                        foodOrder.getPublicId(),
                        foodOrder.getFinalAmount()
                );
                recordOutboxEvent("FoodOrder", foodOrder.getId(), "FOOD_ORDER_CONFIRMED", event);
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



    private void validateOrderModifiable(FoodOrder foodOrder) {
        if (foodOrder.getStatus() != FoodOrderStatus.PENDING) {
            throw new BusinessException("ORDER_NOT_MODIFIABLE", "Food order cannot be modified at this stage");
        }
    }

    private void recordOutboxEvent(String aggregateType, Long aggregateId, String eventType, Object payload) {
        try {
            BookingOutboxEvent event = new BookingOutboxEvent();
            event.setEventId(UUID.randomUUID().toString());
            event.setAggregateType(aggregateType);
            event.setAggregateId(aggregateId);
            event.setEventType(eventType);
            event.setEventVersion(1);
            event.setPayload(objectMapper.writeValueAsString(payload));
            event.setStatus(OutboxStatus.PENDING);
            outboxEventRepository.save(event);
            bookingMetricsManager.incrementOutboxCreated();
        } catch (Exception ex) {
            log.error("Failed to insert outbox event {}: ", eventType, ex);
        }
    }
}
