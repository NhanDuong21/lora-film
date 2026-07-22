package com.lorafilm.booking.food;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.food.client.FoodCatalogClient;
import com.lorafilm.booking.food.dto.response.FoodOrderResponse;
import com.lorafilm.booking.food.entity.FoodOrder;
import com.lorafilm.booking.food.mapper.FoodMapper;
import com.lorafilm.booking.food.repository.FoodOrderItemRepository;
import com.lorafilm.booking.food.repository.FoodOrderRepository;
import com.lorafilm.booking.food.service.impl.FoodOrderServiceImpl;
import com.lorafilm.booking.infrastructure.repository.BookingOutboxEventRepository;
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
class FoodOrderServiceTest {

    @Mock
    private FoodOrderRepository foodOrderRepository;
    
    @Mock
    private FoodOrderItemRepository foodOrderItemRepository;

    @Mock
    private FoodCatalogClient foodCatalogClient;

    @Mock
    private BookingOutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    private FoodOrderServiceImpl foodOrderService;

    @BeforeEach
    void setUp() {
        foodOrderService = new FoodOrderServiceImpl(foodOrderRepository, foodOrderItemRepository, foodCatalogClient, outboxEventRepository, objectMapper);
    }

    @Test
    void shouldCreateFoodOrderSuccessfully() {
        Long userId = 1L;
        Long cinemaId = 1L;
        Long bookingId = 100L;
        String bookingPublicId = "123-abc";
        Long showtimeId = 50L;

        when(foodOrderRepository.save(any(FoodOrder.class))).thenAnswer(i -> {
            FoodOrder order = i.getArgument(0);
            order.setId(10L);
            return order;
        });


        FoodOrderResponse response = foodOrderService.createFoodOrder(bookingId);

        assertNotNull(response);
        verify(foodOrderRepository).save(any(FoodOrder.class));
    }
}
