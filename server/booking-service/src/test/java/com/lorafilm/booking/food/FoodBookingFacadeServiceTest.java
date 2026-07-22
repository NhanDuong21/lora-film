package com.lorafilm.booking.food;

import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.food.dto.request.AddFoodItemRequest;
import com.lorafilm.booking.food.dto.response.FoodOrderResponse;
import com.lorafilm.booking.food.service.FoodOrderService;
import com.lorafilm.booking.food.service.impl.FoodBookingFacadeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FoodBookingFacadeServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    
    @Mock
    private FoodOrderService foodOrderService;

    private FoodBookingFacadeServiceImpl facadeService;

    @BeforeEach
    void setUp() {
        facadeService = new FoodBookingFacadeServiceImpl(bookingRepository, foodOrderService);
    }

    @Test
    void shouldAddFoodItemSuccessfully() {
        String bookingPublicId = "123-abc";
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setPublicId(bookingPublicId);
        booking.setUserId(1L);
        booking.setCinemaId(1L);
        booking.setShowtimeId(1L);
        // Booking defaults to PENDING_PAYMENT, no need to change status.

        when(bookingRepository.findByPublicIdWithLock(bookingPublicId)).thenReturn(Optional.of(booking));
        
        FoodOrderResponse dummyOrder = new FoodOrderResponse();
        dummyOrder.setPublicId("food-123");
        when(foodOrderService.createOrGetFoodOrder(1L)).thenReturn(dummyOrder);
        
        FoodOrderResponse updatedOrder = new FoodOrderResponse();
        updatedOrder.setFinalAmount(new java.math.BigDecimal("150000"));
        when(foodOrderService.addFoodItem(any(), any())).thenReturn(updatedOrder);

        AddFoodItemRequest req = new AddFoodItemRequest();
        FoodOrderResponse response = facadeService.addFoodItem(bookingPublicId, req);

        assertNotNull(response);
        verify(bookingRepository).save(any(Booking.class));
    }
}
