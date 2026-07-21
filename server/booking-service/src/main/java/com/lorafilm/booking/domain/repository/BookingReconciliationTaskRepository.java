package com.lorafilm.booking.domain.repository;

import com.lorafilm.booking.domain.entity.BookingReconciliationTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingReconciliationTaskRepository extends JpaRepository<BookingReconciliationTask, Long>, JpaSpecificationExecutor<BookingReconciliationTask> {
    Optional<BookingReconciliationTask> findByPublicId(UUID publicId);
}
