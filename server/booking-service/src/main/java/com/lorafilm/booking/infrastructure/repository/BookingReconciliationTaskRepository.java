package com.lorafilm.booking.infrastructure.repository;

import com.lorafilm.booking.infrastructure.entity.BookingReconciliationTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingReconciliationTaskRepository extends JpaRepository<BookingReconciliationTask, Long> {

    Optional<BookingReconciliationTask> findByPublicId(String publicId);

    List<BookingReconciliationTask> findByBookingId(Long bookingId);

    Optional<BookingReconciliationTask> findByPaymentEventId(Long paymentEventId);
}
