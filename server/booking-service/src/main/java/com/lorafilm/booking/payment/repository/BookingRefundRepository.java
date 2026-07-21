package com.lorafilm.booking.payment.repository;

import com.lorafilm.booking.payment.entity.BookingRefund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRefundRepository extends JpaRepository<BookingRefund, Long> {

    Optional<BookingRefund> findByPublicId(String publicId);

    Optional<BookingRefund> findByRefundCode(String refundCode);

    List<BookingRefund> findByBookingId(Long bookingId);
}
