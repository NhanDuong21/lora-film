package com.lorafilm.booking.food;

import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.food.client.FoodCatalogClient;
import com.lorafilm.booking.food.client.FoodCatalogItem;
import com.lorafilm.booking.food.dto.request.AddFoodItemRequest;
import com.lorafilm.booking.food.dto.response.FoodOrderResponse;
import com.lorafilm.booking.food.service.impl.FoodBookingFacadeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FoodBookingFacadeServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private FoodCatalogClient foodCatalogClient;

    private FoodBookingFacadeServiceImpl facadeService;

    @BeforeEach
    void setUp() {
        facadeService = new FoodBookingFacadeServiceImpl(bookingRepository, foodCatalogClient);
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
        booking.setExpiresAt(java.time.Instant.now().plusSeconds(900));

        when(bookingRepository.findByPublicIdWithLock(bookingPublicId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AddFoodItemRequest req = new AddFoodItemRequest();
        req.setProductId(999L);
        req.setQuantity(2);

        FoodCatalogItem catalogItem = new FoodCatalogItem();
        catalogItem.setId(999L);
        catalogItem.setName("Popcorn");
        catalogItem.setPrice(new java.math.BigDecimal("50000"));
        catalogItem.setActive(true);
        when(foodCatalogClient.getProductById(999L)).thenReturn(Optional.of(catalogItem));

        FoodOrderResponse response = facadeService.addFoodItem(bookingPublicId, req);

        assertNotNull(response);
        assertEquals(0, response.getFinalAmount().compareTo(new java.math.BigDecimal("100000")));
        verify(bookingRepository).save(any(Booking.class));
    }
}
