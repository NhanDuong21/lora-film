package com.lorafilm.booking.food.service.impl;

import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.common.exception.BusinessException;
import com.lorafilm.booking.common.exception.NotFoundException;
import com.lorafilm.booking.food.client.FoodCatalogClient;
import com.lorafilm.booking.food.client.FoodCatalogItem;
import com.lorafilm.booking.food.dto.request.AddFoodItemRequest;
import com.lorafilm.booking.food.dto.request.UpdateFoodQuantityRequest;
import com.lorafilm.booking.food.dto.response.FoodOrderResponse;
import com.lorafilm.booking.food.mapper.FoodMapper;
import com.lorafilm.booking.food.service.FoodBookingFacadeService;
import com.lorafilm.booking.security.service.SecurityContextService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FoodBookingFacadeServiceImpl implements FoodBookingFacadeService {

    private final BookingRepository bookingRepository;
    private final FoodCatalogClient foodCatalogClient;
    private final SecurityContextService securityContextService;

    @Autowired
    public FoodBookingFacadeServiceImpl(BookingRepository bookingRepository, FoodCatalogClient foodCatalogClient,
                                        SecurityContextService securityContextService) {
        this.bookingRepository = bookingRepository;
        this.foodCatalogClient = foodCatalogClient;
        this.securityContextService = securityContextService;
    }

    public FoodBookingFacadeServiceImpl(BookingRepository bookingRepository, FoodCatalogClient foodCatalogClient) {
        this(bookingRepository, foodCatalogClient, null);
    }

    @Override
    @Transactional(readOnly = true)
    public FoodOrderResponse getFoodOrder(String bookingPublicId) {
        Booking booking = getBooking(bookingPublicId);
        if (booking.getFoodOrder() == null) {
            throw new NotFoundException("FoodOrder", "bookingId", bookingPublicId);
        }
        return FoodMapper.INSTANCE.toFoodOrderResponse(booking.getFoodOrder());
    }

    @Override
    @Transactional
    public FoodOrderResponse addFoodItem(String bookingPublicId, AddFoodItemRequest request) {
        FoodCatalogItem catalogItem = foodCatalogClient.getProductById(request.getProductId())
                .orElseThrow(() -> new BusinessException("FOOD_NOT_FOUND", "Product not found"));

        Booking booking = getBookingForUpdate(bookingPublicId);
        validateBookingStatus(booking);

        booking.addFood(catalogItem, request.getQuantity());
        Booking saved = bookingRepository.save(booking);

        return FoodMapper.INSTANCE.toFoodOrderResponse(saved.getFoodOrder());
    }

    @Override
    @Transactional
    public FoodOrderResponse updateFoodQuantity(String bookingPublicId, Long foodItemId, UpdateFoodQuantityRequest request) {
        Booking booking = getBookingForUpdate(bookingPublicId);
        validateBookingStatus(booking);

        booking.updateFoodQuantity(foodItemId, request.getQuantity());
        Booking saved = bookingRepository.save(booking);

        return FoodMapper.INSTANCE.toFoodOrderResponse(saved.getFoodOrder());
    }

    @Override
    @Transactional
    public void removeFoodItem(String bookingPublicId, Long foodItemId) {
        Booking booking = getBookingForUpdate(bookingPublicId);
        validateBookingStatus(booking);

        booking.removeFoodItem(foodItemId);
        bookingRepository.save(booking);
    }

    private Booking getBooking(String bookingPublicId) {
        return bookingRepository.findByPublicId(bookingPublicId)
                .orElseThrow(() -> new NotFoundException("Booking", "publicId", bookingPublicId));
    }

    private Booking getBookingForUpdate(String bookingPublicId) {
        return bookingRepository.findByPublicIdWithLock(bookingPublicId)
                .orElseThrow(() -> new NotFoundException("Booking", "publicId", bookingPublicId));
    }

    private void validateBookingStatus(Booking booking) {
        if (securityContextService != null
                && securityContextService.getCurrentUserId() != null
                && !securityContextService.getCurrentUserId().equals(booking.getUserId())
                && !securityContextService.isAdmin()) {
            throw new BusinessException("BOOKING_OWNER_REQUIRED", "You do not own this booking");
        }
        if (booking.getBookingStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BusinessException("BOOKING_NOT_MODIFIABLE", "Food cannot be modified because booking status is " + booking.getBookingStatus());
        }
        if (booking.getAmountLockedAt() != null) {
            throw new BusinessException("BOOKING_AMOUNT_LOCKED", "Food cannot be modified after checkout finalization");
        }
        if (booking.getExpiresAt() == null || !booking.getExpiresAt().isAfter(java.time.Instant.now())) {
            throw new BusinessException("BOOKING_EXPIRED", "The Booking payment deadline has passed");
        }
    }
}
