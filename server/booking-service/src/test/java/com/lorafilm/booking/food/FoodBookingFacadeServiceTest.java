package com.lorafilm.booking.food;

import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.food.client.FoodCatalogClient;
import com.lorafilm.booking.food.client.FoodCatalogItem;
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

    @Mock
    private FoodCatalogClient foodCatalogClient;

    private FoodBookingFacadeServiceImpl facadeService;

    @BeforeEach
    void setUp() {
        facadeService = new FoodBookingFacadeServiceImpl(bookingRepository, foodOrderService, foodCatalogClient);
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

        AddFoodItemRequest req = new AddFoodItemRequest();
        req.setProductId(999L);
        req.setQuantity(2);

        FoodCatalogItem catalogItem = new FoodCatalogItem();
        catalogItem.setId(999L);
        when(foodCatalogClient.getProductById(999L)).thenReturn(Optional.of(catalogItem));

        when(foodOrderService.addFoodItem(any(), any(), any(Integer.class))).thenReturn(updatedOrder);

        FoodOrderResponse response = facadeService.addFoodItem(bookingPublicId, req);

        assertNotNull(response);
        verify(bookingRepository).save(any(Booking.class));
    }
}
