package com.lorafilm.booking.food;

import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.food.dto.request.AddFoodItemRequest;
import com.lorafilm.booking.food.dto.response.FoodOrderResponse;
import com.lorafilm.booking.food.entity.BookingFoodItem;
import com.lorafilm.booking.food.entity.BookingFoodOrder;
import com.lorafilm.booking.food.exception.FoodErrorCode;
import com.lorafilm.booking.food.exception.FoodException;
import com.lorafilm.booking.food.mapper.FoodMapper;
import com.lorafilm.booking.food.repository.BookingFoodItemRepository;
import com.lorafilm.booking.food.repository.BookingFoodOrderRepository;
import com.lorafilm.booking.food.service.impl.BookingFoodItemServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.access.AccessDeniedException;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingFoodItemServiceTest {

    @Mock
    private BookingFoodItemRepository foodItemRepository;

    @Mock
    private BookingFoodOrderRepository foodOrderRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private FoodMapper foodMapper;

    @Mock
    private com.lorafilm.booking.food.client.FoodCatalogClient foodCatalogClient;

    @Mock
    private com.lorafilm.booking.security.service.SecurityContextService securityContextService;

    private BookingFoodItemServiceImpl foodItemService;

    @BeforeEach
    void setUp() {
        foodItemService = new BookingFoodItemServiceImpl(foodItemRepository, foodOrderRepository, bookingRepository, foodMapper, foodCatalogClient, securityContextService);
    }

    @Test
    void shouldAddFoodItemSuccessfully() {
        String bookingId = "123-abc";
        Booking booking = Booking.create(bookingId, "CODE-1", 1L, 1L, 1L, 1L, 1L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "VND", java.time.Instant.now().plusSeconds(3600), "");
        booking.setId(1L);

        BookingFoodOrder order = new BookingFoodOrder();
        order.setId(10L);

        AddFoodItemRequest request = new AddFoodItemRequest();
        request.setProductId(100L);
        request.setQuantity(2);

        when(bookingRepository.findByPublicIdWithLock(bookingId)).thenReturn(Optional.of(booking));
        when(securityContextService.getCurrentUserId()).thenReturn(1L);
        when(foodOrderRepository.findByBookingId(1L)).thenReturn(Optional.of(order));
        
        com.lorafilm.booking.food.client.FoodCatalogItem catalogItem = new com.lorafilm.booking.food.client.FoodCatalogItem(
                100L, "CODE", "Name", com.lorafilm.booking.food.enums.ProductType.FOOD, "url", new BigDecimal("50000.00"));
        when(foodCatalogClient.getProductById(100L)).thenReturn(Optional.of(catalogItem));

        BookingFoodItem savedItem = new BookingFoodItem();
        savedItem.setQuantity(2);
        savedItem.setSubtotal(new BigDecimal("100000.00"));
        savedItem.setFinalAmount(new BigDecimal("100000.00"));

        when(foodItemRepository.findByFoodOrderId(10L)).thenReturn(List.of(savedItem));
        when(foodMapper.toFoodOrderResponse(order)).thenReturn(new FoodOrderResponse());

        foodItemService.addFoodItem(bookingId, request);

        verify(foodItemRepository).save(any(BookingFoodItem.class));
        verify(foodOrderRepository).save(order);
        verify(bookingRepository).save(booking);
        
        assertEquals(new BigDecimal("100000.00"), booking.getFoodAmount());
    }

    @Test
    void shouldRejectAddFoodItemWhenBookingConfirmed() {
        String bookingId = "123-abc";
        Booking booking = Booking.create(bookingId, "CODE-1", 1L, 1L, 1L, 1L, 1L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "VND", java.time.Instant.now().plusSeconds(3600), "");
        booking.setId(1L);
        booking.changeStatus(BookingStatus.CONFIRMED, java.time.Instant.now());

        AddFoodItemRequest request = new AddFoodItemRequest();

        when(bookingRepository.findByPublicIdWithLock(bookingId)).thenReturn(Optional.of(booking));
        when(securityContextService.getCurrentUserId()).thenReturn(1L);

        FoodException exception = assertThrows(FoodException.class, () -> foodItemService.addFoodItem(bookingId, request));
        assertEquals(FoodErrorCode.BOOKING_NOT_ALLOW_FOOD_MODIFICATION, exception.getErrorCode());
    }

    @Test
    void shouldRejectWhenUserDoesNotOwnBooking_IDOR() {
        String bookingId = "123-abc";
        Booking booking = Booking.create(bookingId, "CODE-1", 1L, 2L, 1L, 1L, 1L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "VND", java.time.Instant.now().plusSeconds(3600), "");
        booking.setId(1L);

        when(bookingRepository.findByPublicIdWithLock(bookingId)).thenReturn(Optional.of(booking));
        when(securityContextService.getCurrentUserId()).thenReturn(999L); // Malicious user

        assertThrows(AccessDeniedException.class, () -> foodItemService.addFoodItem(bookingId, new AddFoodItemRequest()));
    }

    @Test
    void shouldRejectWhenProductNotSellable() {
        String bookingId = "123-abc";
        Booking booking = Booking.create(bookingId, "CODE-1", 1L, 1L, 1L, 1L, 1L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "VND", java.time.Instant.now().plusSeconds(3600), "");
        booking.setId(1L);

        AddFoodItemRequest request = new AddFoodItemRequest();
        request.setProductId(100L);
        request.setQuantity(1);

        when(bookingRepository.findByPublicIdWithLock(bookingId)).thenReturn(Optional.of(booking));
        when(securityContextService.getCurrentUserId()).thenReturn(1L);
        
        BookingFoodOrder order = new BookingFoodOrder();
        order.setId(10L);
        when(foodOrderRepository.findByBookingId(1L)).thenReturn(Optional.of(order));

        com.lorafilm.booking.food.client.FoodCatalogItem catalogItem = new com.lorafilm.booking.food.client.FoodCatalogItem(
                100L, "CODE", "Name", com.lorafilm.booking.food.enums.ProductType.FOOD, "url", new BigDecimal("50000.00"), true, false, false, false, "VND"); // sellable = false
        when(foodCatalogClient.getProductById(100L)).thenReturn(Optional.of(catalogItem));

        FoodException exception = assertThrows(FoodException.class, () -> foodItemService.addFoodItem(bookingId, request));
        assertEquals(FoodErrorCode.INVALID_PRODUCT, exception.getErrorCode());
    }

    @Test
    void shouldRejectWhenProductIsInactive() {
        String bookingId = "123-abc";
        Booking booking = Booking.create(bookingId, "CODE-1", 1L, 1L, 1L, 1L, 1L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "VND", java.time.Instant.now().plusSeconds(3600), "");
        booking.setId(1L);

        AddFoodItemRequest request = new AddFoodItemRequest();
        request.setProductId(100L);
        request.setQuantity(1);

        when(bookingRepository.findByPublicIdWithLock(bookingId)).thenReturn(Optional.of(booking));
        when(securityContextService.getCurrentUserId()).thenReturn(1L);
        
        BookingFoodOrder order = new BookingFoodOrder();
        order.setId(10L);
        when(foodOrderRepository.findByBookingId(1L)).thenReturn(Optional.of(order));

        com.lorafilm.booking.food.client.FoodCatalogItem catalogItem = new com.lorafilm.booking.food.client.FoodCatalogItem(
                100L, "CODE", "Name", com.lorafilm.booking.food.enums.ProductType.FOOD, "url", new BigDecimal("50000.00"), false, true, false, false, "VND"); // active = false
        when(foodCatalogClient.getProductById(100L)).thenReturn(Optional.of(catalogItem));

        FoodException exception = assertThrows(FoodException.class, () -> foodItemService.addFoodItem(bookingId, request));
        assertEquals(FoodErrorCode.INVALID_PRODUCT, exception.getErrorCode());
    }

    @Test
    void shouldRejectZeroOrNegativeQuantity() {
        String bookingId = "123-abc";
        Booking booking = Booking.create(bookingId, "CODE-1", 1L, 1L, 1L, 1L, 1L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "VND", java.time.Instant.now().plusSeconds(3600), "");
        booking.setId(1L);

        AddFoodItemRequest request = new AddFoodItemRequest();
        request.setProductId(100L);
        request.setQuantity(-5); // Negative quantity

        when(bookingRepository.findByPublicIdWithLock(bookingId)).thenReturn(Optional.of(booking));
        when(securityContextService.getCurrentUserId()).thenReturn(1L);
        
        BookingFoodOrder order = new BookingFoodOrder();
        order.setId(10L);
        when(foodOrderRepository.findByBookingId(1L)).thenReturn(Optional.of(order));

        com.lorafilm.booking.food.client.FoodCatalogItem catalogItem = new com.lorafilm.booking.food.client.FoodCatalogItem(
                100L, "CODE", "Name", com.lorafilm.booking.food.enums.ProductType.FOOD, "url", new BigDecimal("50000.00"));
        when(foodCatalogClient.getProductById(100L)).thenReturn(Optional.of(catalogItem));

        FoodException exception = assertThrows(FoodException.class, () -> foodItemService.addFoodItem(bookingId, request));
        assertEquals(FoodErrorCode.INVALID_PRODUCT, exception.getErrorCode());
    }
}
