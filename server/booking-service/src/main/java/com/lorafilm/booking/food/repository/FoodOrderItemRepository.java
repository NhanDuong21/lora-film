package com.lorafilm.booking.food.repository;

import com.lorafilm.booking.food.dto.response.FoodItemSalesDto;
import com.lorafilm.booking.food.entity.FoodOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodOrderItemRepository extends JpaRepository<FoodOrderItem, Long> {

    @Query("SELECT new com.lorafilm.booking.food.dto.response.FoodItemSalesDto(" +
           "item.productName, item.productCode, item.productType, " +
           "SUM(CAST(item.quantity AS long)), SUM(item.finalAmount)) " +
           "FROM FoodOrderItem item " +
           "WHERE item.foodOrder.booking.bookingStatus = com.lorafilm.booking.booking.enums.BookingStatus.CONFIRMED " +
           "OR item.foodOrder.booking.bookingStatus = com.lorafilm.booking.booking.enums.BookingStatus.COMPLETED " +
           "GROUP BY item.productName, item.productCode, item.productType")
    List<FoodItemSalesDto> getFoodSalesStatistics();
}
