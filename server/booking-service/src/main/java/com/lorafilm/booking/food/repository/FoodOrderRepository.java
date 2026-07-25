package com.lorafilm.booking.food.repository;

import com.lorafilm.booking.food.entity.FoodOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FoodOrderRepository extends JpaRepository<FoodOrder, Long> {
    Optional<FoodOrder> findByPublicId(String publicId);

    @Query("SELECT f FROM FoodOrder f WHERE f.booking.id = :bookingId")
    Optional<FoodOrder> findByBookingId(@Param("bookingId") Long bookingId);
}
