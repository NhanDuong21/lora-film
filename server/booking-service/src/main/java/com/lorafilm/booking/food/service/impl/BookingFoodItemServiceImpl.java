package com.lorafilm.booking.food.service.impl;

import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.common.exception.NotFoundException;
import com.lorafilm.booking.food.dto.request.AddFoodItemRequest;
import com.lorafilm.booking.food.dto.request.UpdateFoodQuantityRequest;
import com.lorafilm.booking.food.dto.response.FoodOrderResponse;
import com.lorafilm.booking.food.entity.BookingFoodItem;
import com.lorafilm.booking.food.entity.BookingFoodOrder;
import com.lorafilm.booking.food.enums.ProductType;
import com.lorafilm.booking.food.exception.FoodErrorCode;
import com.lorafilm.booking.food.exception.FoodException;
import com.lorafilm.booking.food.mapper.FoodMapper;
import com.lorafilm.booking.food.repository.BookingFoodItemRepository;
import com.lorafilm.booking.food.repository.BookingFoodOrderRepository;
import com.lorafilm.booking.food.service.BookingFoodItemService;
import com.lorafilm.booking.food.client.FoodCatalogClient;
import com.lorafilm.booking.food.client.FoodCatalogItem;
import com.lorafilm.booking.security.service.SecurityContextService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional
public class BookingFoodItemServiceImpl implements BookingFoodItemService {

    private final BookingFoodItemRepository foodItemRepository;
    private final BookingFoodOrderRepository foodOrderRepository;
    private final BookingRepository bookingRepository;
    private final FoodMapper foodMapper;
    private final FoodCatalogClient foodCatalogClient;
    private final SecurityContextService securityContextService;
    private final ObjectMapper objectMapper;

    public BookingFoodItemServiceImpl(BookingFoodItemRepository foodItemRepository,
                                      BookingFoodOrderRepository foodOrderRepository,
                                      BookingRepository bookingRepository,
                                      FoodMapper foodMapper,
                                      FoodCatalogClient foodCatalogClient,
                                      SecurityContextService securityContextService) {
        this.foodItemRepository = foodItemRepository;
        this.foodOrderRepository = foodOrderRepository;
        this.bookingRepository = bookingRepository;
        this.foodMapper = foodMapper;
        this.foodCatalogClient = foodCatalogClient;
        this.securityContextService = securityContextService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public FoodOrderResponse addFoodItem(String bookingId, AddFoodItemRequest request) {
        Booking booking = getBookingAndValidateStatus(bookingId);
        BookingFoodOrder foodOrder = getOrCreateFoodOrder(booking);

        FoodCatalogItem catalogItem = foodCatalogClient.getProductById(request.getProductId())
                .orElseThrow(() -> new FoodException(FoodErrorCode.INVALID_PRODUCT, "Invalid product or product not available"));

        // Strict Business Validations
        if (!catalogItem.isActive() || !catalogItem.isSellable() || catalogItem.isDeleted() || catalogItem.isDisabled()) {
            throw new FoodException(FoodErrorCode.INVALID_PRODUCT, "Product is currently unavailable for sale");
        }
        if (catalogItem.getPrice() == null || catalogItem.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new FoodException(FoodErrorCode.INVALID_PRODUCT, "Product price is invalid");
        }
        if (catalogItem.getType() == null || catalogItem.getName() == null || catalogItem.getName().trim().isEmpty()) {
            throw new FoodException(FoodErrorCode.INVALID_PRODUCT, "Product details are incomplete");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new FoodException(FoodErrorCode.INVALID_PRODUCT, "Quantity must be greater than zero");
        }

        BookingFoodItem foodItem = new BookingFoodItem();
        foodItem.setFoodOrder(foodOrder);
        foodItem.setProductId(catalogItem.getId());
        foodItem.setProductCode(catalogItem.getCode());
        foodItem.setProductName(catalogItem.getName());
        foodItem.setProductType(catalogItem.getType());
        foodItem.setProductImage(catalogItem.getImageUrl());
        foodItem.setQuantity(request.getQuantity());
        foodItem.setUnitPrice(catalogItem.getPrice());
        foodItem.setCurrency(catalogItem.getCurrency() != null ? catalogItem.getCurrency() : "VND");
        
        try {
            foodItem.setSnapshotJson(objectMapper.writeValueAsString(catalogItem));
        } catch (Exception e) {
            // fallback if serialization fails
            foodItem.setSnapshotJson("{}");
        }
        
        BigDecimal subtotal = catalogItem.getPrice().multiply(BigDecimal.valueOf(request.getQuantity())).setScale(2, RoundingMode.HALF_UP);
        foodItem.setSubtotal(subtotal);
        foodItem.setFinalAmount(subtotal); // discount logic can be added later

        foodItemRepository.save(foodItem);

        updateFoodOrderTotals(foodOrder);
        updateBookingFoodAmount(booking, foodOrder.getFinalAmount());

        return foodMapper.toFoodOrderResponse(foodOrder);
    }

    @Override
    public FoodOrderResponse updateQuantity(String bookingId, Long foodItemId, UpdateFoodQuantityRequest request) {
        Booking booking = getBookingAndValidateStatus(bookingId);
        BookingFoodOrder foodOrder = getFoodOrder(booking);

        BookingFoodItem foodItem = foodItemRepository.findById(foodItemId)
                .orElseThrow(() -> new FoodException(FoodErrorCode.FOOD_ITEM_NOT_FOUND, "Food item not found"));

        if (!foodItem.getFoodOrder().getId().equals(foodOrder.getId())) {
            throw new FoodException(FoodErrorCode.FOOD_ITEM_NOT_FOUND, "Food item does not belong to this order");
        }

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new FoodException(FoodErrorCode.INVALID_PRODUCT, "Quantity must be greater than zero");
        }

        foodItem.setQuantity(request.getQuantity());
        BigDecimal subtotal = foodItem.getUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity())).setScale(2, RoundingMode.HALF_UP);
        foodItem.setSubtotal(subtotal);
        foodItem.setFinalAmount(subtotal.subtract(foodItem.getDiscountAmount()).setScale(2, RoundingMode.HALF_UP));

        foodItemRepository.save(foodItem);

        updateFoodOrderTotals(foodOrder);
        updateBookingFoodAmount(booking, foodOrder.getFinalAmount());

        return foodMapper.toFoodOrderResponse(foodOrder);
    }

    @Override
    public FoodOrderResponse removeFoodItem(String bookingId, Long foodItemId) {
        Booking booking = getBookingAndValidateStatus(bookingId);
        BookingFoodOrder foodOrder = getFoodOrder(booking);

        BookingFoodItem foodItem = foodItemRepository.findById(foodItemId)
                .orElseThrow(() -> new FoodException(FoodErrorCode.FOOD_ITEM_NOT_FOUND, "Food item not found"));

        if (!foodItem.getFoodOrder().getId().equals(foodOrder.getId())) {
            throw new FoodException(FoodErrorCode.FOOD_ITEM_NOT_FOUND, "Food item does not belong to this order");
        }

        foodItemRepository.delete(foodItem);

        updateFoodOrderTotals(foodOrder);
        updateBookingFoodAmount(booking, foodOrder.getFinalAmount());

        return foodMapper.toFoodOrderResponse(foodOrder);
    }

    private Booking getBookingAndValidateStatus(String bookingId) {
        Booking booking = bookingRepository.findByPublicIdWithLock(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found with publicId: " + bookingId));

        Long currentUserId = securityContextService.getCurrentUserId();
        if (currentUserId != null && !booking.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("Unauthorized to access or modify this booking");
        }

        if (booking.getBookingStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new FoodException(FoodErrorCode.BOOKING_NOT_ALLOW_FOOD_MODIFICATION, 
                    "Food can only be modified when booking is PENDING_PAYMENT");
        }
        return booking;
    }

    private BookingFoodOrder getOrCreateFoodOrder(Booking booking) {
        return foodOrderRepository.findByBookingId(booking.getId())
                .orElseGet(() -> {
                    BookingFoodOrder newOrder = new BookingFoodOrder();
                    newOrder.setPublicId(UUID.randomUUID().toString());
                    newOrder.setBooking(booking);
                    return foodOrderRepository.save(newOrder);
                });
    }

    private BookingFoodOrder getFoodOrder(Booking booking) {
        return foodOrderRepository.findByBookingId(booking.getId())
                .orElseThrow(() -> new FoodException(FoodErrorCode.FOOD_ORDER_NOT_FOUND, "Food order not found for this booking"));
    }

    private void updateFoodOrderTotals(BookingFoodOrder foodOrder) {
        var items = foodItemRepository.findByFoodOrderId(foodOrder.getId());
        
        int totalQuantity = items.stream().mapToInt(BookingFoodItem::getQuantity).sum();
        BigDecimal subtotal = items.stream().map(BookingFoodItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountAmount = items.stream().map(BookingFoodItem::getDiscountAmount).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalAmount = items.stream().map(BookingFoodItem::getFinalAmount).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);

        foodOrder.setTotalQuantity(totalQuantity);
        foodOrder.setSubtotal(subtotal);
        foodOrder.setDiscountAmount(discountAmount);
        foodOrder.setFinalAmount(finalAmount);

        foodOrderRepository.save(foodOrder);
    }

    private void updateBookingFoodAmount(Booking booking, BigDecimal foodAmount) {
        booking.updateFoodAmount(foodAmount);
        bookingRepository.save(booking);
    }
}
