package com.lorafilm.booking.payment.repository;

import com.lorafilm.booking.payment.entity.BookingPaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingPaymentEventRepository extends JpaRepository<BookingPaymentEvent, Long> {

    Optional<BookingPaymentEvent> findByPublicId(String publicId);

    List<BookingPaymentEvent> findByBookingId(Long bookingId);

    boolean existsByBookingId(Long bookingId);

    @Query("select distinct event.booking.id from BookingPaymentEvent event where event.booking.id in :bookingIds")
    List<Long> findBookingIdsWithEvents(@Param("bookingIds") Collection<Long> bookingIds);
}
