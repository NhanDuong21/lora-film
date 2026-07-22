package com.lorafilm.booking.food.service.impl;

import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.common.exception.NotFoundException;
import com.lorafilm.booking.food.dto.response.FoodOrderResponse;
import com.lorafilm.booking.food.entity.BookingFoodOrder;
import com.lorafilm.booking.food.exception.FoodErrorCode;
import com.lorafilm.booking.food.exception.FoodException;
import com.lorafilm.booking.food.mapper.FoodMapper;
import com.lorafilm.booking.food.repository.BookingFoodOrderRepository;
import com.lorafilm.booking.food.service.BookingFoodOrderService;
import com.lorafilm.booking.security.service.SecurityContextService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Transactional
public class BookingFoodOrderServiceImpl implements BookingFoodOrderService {

    private final BookingFoodOrderRepository foodOrderRepository;
    private final BookingRepository bookingRepository;
    private final FoodMapper foodMapper;
    private final SecurityContextService securityContextService;

    public BookingFoodOrderServiceImpl(BookingFoodOrderRepository foodOrderRepository,
                                       BookingRepository bookingRepository,
                                       FoodMapper foodMapper,
                                       SecurityContextService securityContextService) {
        this.foodOrderRepository = foodOrderRepository;
        this.bookingRepository = bookingRepository;
        this.foodMapper = foodMapper;
        this.securityContextService = securityContextService;
    }

    private Booking getBookingAndValidateOwnership(String bookingId) {
        Booking booking = bookingRepository.findByPublicId(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found with publicId: " + bookingId));

        Long currentUserId = securityContextService.getCurrentUserId();
        if (currentUserId != null && !booking.getUserId().equals(currentUserId)) {
            // Check if user is an admin. If not, throw exception.
            if (!securityContextService.isAdmin()) {
                throw new AccessDeniedException("Unauthorized to access this booking");
            }
        }
        return booking;
    }

    @Override
    public FoodOrderResponse createFoodOrder(String bookingId) {
        Booking booking = getBookingAndValidateOwnership(bookingId);

        if (foodOrderRepository.findByBookingId(booking.getId()).isPresent()) {
            throw new FoodException(FoodErrorCode.FOOD_ORDER_ALREADY_CONFIRMED, "Food order already exists for this booking");
        }

        BookingFoodOrder foodOrder = new BookingFoodOrder();
        foodOrder.setPublicId(UUID.randomUUID().toString());
        foodOrder.setBooking(booking);

        return foodMapper.toFoodOrderResponse(foodOrderRepository.save(foodOrder));
    }

    @Override
    @Transactional(readOnly = true)
    public FoodOrderResponse getFoodOrder(String bookingId) {
        Booking booking = getBookingAndValidateOwnership(bookingId);

        BookingFoodOrder foodOrder = foodOrderRepository.findByBookingId(booking.getId())
                .orElseThrow(() -> new FoodException(FoodErrorCode.FOOD_ORDER_NOT_FOUND, "Food order not found for this booking"));

        return foodMapper.toFoodOrderResponse(foodOrder);
    }

    @Override
    public void removeFoodOrder(String bookingId) {
        Booking booking = getBookingAndValidateOwnership(bookingId);

        BookingFoodOrder foodOrder = foodOrderRepository.findByBookingId(booking.getId())
                .orElseThrow(() -> new FoodException(FoodErrorCode.FOOD_ORDER_NOT_FOUND, "Food order not found for this booking"));

        foodOrderRepository.delete(foodOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateFoodAmount(String bookingId) {
        Booking booking = getBookingAndValidateOwnership(bookingId);

        return foodOrderRepository.findByBookingId(booking.getId())
                .map(BookingFoodOrder::getFinalAmount)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public Object getFoodStatistics() {
        // Dummy implementation for now
        return java.util.Map.of("totalOrders", foodOrderRepository.count());
    }
}
