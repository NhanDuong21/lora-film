package com.lorafilm.booking.food.repository;

import com.lorafilm.booking.food.entity.FoodOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FoodOrderRepository extends JpaRepository<FoodOrder, Long> {
    Optional<FoodOrder> findByPublicId(String publicId);
    Optional<FoodOrder> findByBookingId(Long bookingId);
}
