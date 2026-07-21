package com.lorafilm.booking.domain.repository;

import com.lorafilm.booking.domain.entity.BookingStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingStatusHistoryRepository extends JpaRepository<BookingStatusHistory, Long>, JpaSpecificationExecutor<BookingStatusHistory> {
    List<BookingStatusHistory> findByBookingId(Long bookingId);
}
