package com.lorafilm.booking.food;

import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.common.exception.NotFoundException;
import com.lorafilm.booking.food.dto.response.FoodOrderResponse;
import com.lorafilm.booking.food.entity.BookingFoodOrder;
import com.lorafilm.booking.food.exception.FoodErrorCode;
import com.lorafilm.booking.food.exception.FoodException;
import com.lorafilm.booking.food.mapper.FoodMapper;
import com.lorafilm.booking.food.repository.BookingFoodOrderRepository;
import com.lorafilm.booking.food.service.impl.BookingFoodOrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingFoodOrderServiceTest {

    @Mock
    private BookingFoodOrderRepository foodOrderRepository;
    
    @Mock
    private BookingRepository bookingRepository;
    
    @Mock
    private FoodMapper foodMapper;

    @Mock
    private com.lorafilm.booking.security.service.SecurityContextService securityContextService;

    private BookingFoodOrderServiceImpl foodOrderService;

    @BeforeEach
    void setUp() {
        foodOrderService = new BookingFoodOrderServiceImpl(foodOrderRepository, bookingRepository, foodMapper, securityContextService);
    }

    @Test
    void shouldCreateFoodOrderSuccessfully() {
        String bookingId = "123-abc";
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setPublicId(bookingId);
        booking.setUserId(1L);

        when(bookingRepository.findByPublicId(bookingId)).thenReturn(Optional.of(booking));
        when(securityContextService.getCurrentUserId()).thenReturn(1L);
        when(foodOrderRepository.findByBookingId(1L)).thenReturn(Optional.empty());
        when(foodOrderRepository.save(any(BookingFoodOrder.class))).thenAnswer(i -> {
            BookingFoodOrder order = i.getArgument(0);
            order.setId(10L);
            return order;
        });

        FoodOrderResponse dummyResponse = new FoodOrderResponse();
        when(foodMapper.toFoodOrderResponse(any(BookingFoodOrder.class))).thenReturn(dummyResponse);

        FoodOrderResponse response = foodOrderService.createFoodOrder(bookingId);

        assertEquals(dummyResponse, response);
        verify(foodOrderRepository).save(any(BookingFoodOrder.class));
    }

    @Test
    void shouldThrowExceptionIfOrderExists() {
        String bookingId = "123-abc";
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setUserId(1L);

        when(bookingRepository.findByPublicId(bookingId)).thenReturn(Optional.of(booking));
        when(securityContextService.getCurrentUserId()).thenReturn(1L);
        when(foodOrderRepository.findByBookingId(1L)).thenReturn(Optional.of(new BookingFoodOrder()));

        FoodException exception = assertThrows(FoodException.class, () -> foodOrderService.createFoodOrder(bookingId));
        assertEquals(FoodErrorCode.FOOD_ORDER_ALREADY_CONFIRMED, exception.getErrorCode());
    }

    @Test
    void shouldCalculateFoodAmount() {
        String bookingId = "123-abc";
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setUserId(1L);

        BookingFoodOrder order = new BookingFoodOrder();
        order.setFinalAmount(new BigDecimal("150000"));

        when(bookingRepository.findByPublicId(bookingId)).thenReturn(Optional.of(booking));
        when(securityContextService.getCurrentUserId()).thenReturn(1L);
        when(foodOrderRepository.findByBookingId(1L)).thenReturn(Optional.of(order));

        BigDecimal amount = foodOrderService.calculateFoodAmount(bookingId);
        assertEquals(new BigDecimal("150000"), amount);
    }
}
