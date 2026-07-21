package com.lorafilm.booking.food.repository;

import com.lorafilm.booking.food.entity.BookingFoodOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookingFoodOrderRepository extends JpaRepository<BookingFoodOrder, Long> {

    Optional<BookingFoodOrder> findByPublicId(String publicId);

    Optional<BookingFoodOrder> findByBookingId(Long bookingId);
}
