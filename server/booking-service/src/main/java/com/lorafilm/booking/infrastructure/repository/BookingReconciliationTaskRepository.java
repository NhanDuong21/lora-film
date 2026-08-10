package com.lorafilm.booking.infrastructure.repository;

import com.lorafilm.booking.infrastructure.entity.BookingReconciliationTask;
import com.lorafilm.booking.infrastructure.enums.ReconciliationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingReconciliationTaskRepository extends JpaRepository<BookingReconciliationTask, Long> {

    Optional<BookingReconciliationTask> findByPublicId(String publicId);

    List<BookingReconciliationTask> findByBookingId(Long bookingId);

    Optional<BookingReconciliationTask> findByPaymentEventId(Long paymentEventId);

    Optional<BookingReconciliationTask>
    findFirstByBookingIdAndPaymentReferenceAndReasonStartingWithOrderByIdDesc(
            Long bookingId, String paymentReference, String reasonPrefix);

    @Query("""
            select task from BookingReconciliationTask task
            join fetch task.booking booking
            where task.reconciliationStatus in :statuses
              and task.reason like concat(:reasonPrefix, '%')
              and coalesce(task.checkedAt, task.createdAt) <= :cutoff
            order by coalesce(task.checkedAt, task.createdAt) asc, task.id asc
            """)
    List<BookingReconciliationTask> findPromotionTasksForRecheck(
            @Param("statuses") Collection<ReconciliationStatus> statuses,
            @Param("reasonPrefix") String reasonPrefix,
            @Param("cutoff") Instant cutoff,
            Pageable pageable);

    long countByReconciliationStatusAndReasonStartingWith(
            ReconciliationStatus status, String reasonPrefix);
}
