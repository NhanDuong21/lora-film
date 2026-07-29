package com.lorafilm.booking.payment.repository;

import com.lorafilm.booking.payment.entity.BookingRefund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

@Repository
public interface BookingRefundRepository extends JpaRepository<BookingRefund, Long> {

    Optional<BookingRefund> findByPublicId(String publicId);

    Optional<BookingRefund> findByRefundCode(String refundCode);

    List<BookingRefund> findByBookingId(Long bookingId);

    @Query("""
            select coalesce(sum(r.refundAmount), 0)
            from BookingRefund r
            where r.booking.id = :bookingId
              and r.status = com.lorafilm.booking.payment.enums.RefundStatus.SUCCESS
            """)
    BigDecimal sumSuccessfulAmountByBookingId(@Param("bookingId") Long bookingId);
}
