package com.lorafilm.booking.food.repository;

import com.lorafilm.booking.food.entity.BookingFoodItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingFoodItemRepository extends JpaRepository<BookingFoodItem, Long> {

    List<BookingFoodItem> findByFoodOrderId(Long foodOrderId);
}
