package com.lorafilm.booking.food.repository;

import com.lorafilm.booking.food.entity.FoodOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FoodOrderItemRepository extends JpaRepository<FoodOrderItem, Long> {
}
