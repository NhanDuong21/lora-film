package com.lorafilm.booking.domain.repository;

import com.lorafilm.booking.domain.entity.BookingRetryTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRetryTaskRepository extends JpaRepository<BookingRetryTask, Long>, JpaSpecificationExecutor<BookingRetryTask> {
    Optional<BookingRetryTask> findByPublicId(UUID publicId);
}
