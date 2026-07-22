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
import com.lorafilm.booking.food.service.FoodBookingFacadeService;
import com.lorafilm.booking.food.service.FoodOrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FoodBookingFacadeServiceImpl implements FoodBookingFacadeService {

    private final BookingRepository bookingRepository;
    private final FoodOrderService foodOrderService;
    private final FoodCatalogClient foodCatalogClient;

    public FoodBookingFacadeServiceImpl(BookingRepository bookingRepository, FoodOrderService foodOrderService, FoodCatalogClient foodCatalogClient) {
        this.bookingRepository = bookingRepository;
        this.foodOrderService = foodOrderService;
        this.foodCatalogClient = foodCatalogClient;
    }

    @Override
    @Transactional(readOnly = true)
    public FoodOrderResponse getFoodOrder(String bookingPublicId) {
        Booking booking = getBooking(bookingPublicId);
        FoodOrderResponse response = foodOrderService.getFoodOrderByBookingId(booking.getId());
        if (response == null) {
            throw new NotFoundException("FoodOrder", "bookingId", bookingPublicId);
        }
        return response;
    }

    @Override
    @Transactional
    public FoodOrderResponse addFoodItem(String bookingPublicId, AddFoodItemRequest request) {
        // Fetch external data BEFORE acquiring the pessimistic lock on the Booking.
        // This drastically reduces the lock duration and prevents thread starvation during high concurrency.
        FoodCatalogItem catalogItem = foodCatalogClient.getProductById(request.getProductId())
                .orElseThrow(() -> new BusinessException("FOOD_NOT_FOUND", "Product not found"));

        Booking booking = getBookingForUpdate(bookingPublicId);
        validateBookingStatus(booking);

        FoodOrderResponse foodOrder = foodOrderService.createOrGetFoodOrder(booking.getId());

        FoodOrderResponse updatedOrder = foodOrderService.addFoodItem(foodOrder.getPublicId(), catalogItem, request.getQuantity());
        
        updateBookingTotal(booking, updatedOrder);
        return updatedOrder;
    }

    @Override
    @Transactional
    public FoodOrderResponse updateFoodQuantity(String bookingPublicId, Long foodItemId, UpdateFoodQuantityRequest request) {
        Booking booking = getBookingForUpdate(bookingPublicId);
        validateBookingStatus(booking);

        FoodOrderResponse foodOrder = getFoodOrder(bookingPublicId);
        FoodOrderResponse updatedOrder = foodOrderService.updateFoodQuantity(foodOrder.getPublicId(), foodItemId, request);
        
        updateBookingTotal(booking, updatedOrder);
        return updatedOrder;
    }

    @Override
    @Transactional
    public void removeFoodItem(String bookingPublicId, Long foodItemId) {
        Booking booking = getBookingForUpdate(bookingPublicId);
        validateBookingStatus(booking);

        FoodOrderResponse foodOrder = getFoodOrder(bookingPublicId);
        foodOrderService.removeFoodItem(foodOrder.getPublicId(), foodItemId);
        
        FoodOrderResponse updatedOrder = foodOrderService.getFoodOrder(foodOrder.getPublicId());
        updateBookingTotal(booking, updatedOrder);
    }

    private Booking getBooking(String bookingPublicId) {
        return bookingRepository.findByPublicId(bookingPublicId)
                .orElseThrow(() -> new NotFoundException("Booking", "publicId", bookingPublicId));
    }

    private Booking getBookingForUpdate(String bookingPublicId) {
        // We use PESSIMISTIC_WRITE here specifically to ensure Booking final_amount calculation is safe.
        // The FoodOrder itself has optimistic locking, so we maintain data integrity across boundaries.
        return bookingRepository.findByPublicIdWithLock(bookingPublicId)
                .orElseThrow(() -> new NotFoundException("Booking", "publicId", bookingPublicId));
    }

    private void validateBookingStatus(Booking booking) {
        if (booking.getBookingStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BusinessException("BOOKING_NOT_MODIFIABLE", "Food cannot be modified because booking status is " + booking.getBookingStatus());
        }
    }

    private void updateBookingTotal(Booking booking, FoodOrderResponse foodOrder) {
        booking.updateFoodAmount(foodOrder.getFinalAmount());
        bookingRepository.save(booking);
    }
}
