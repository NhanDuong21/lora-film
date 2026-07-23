package com.lorafilm.booking.infrastructure.repository;

import com.lorafilm.booking.infrastructure.entity.BookingRetryTask;
import com.lorafilm.booking.infrastructure.enums.RetryTaskStatus;
import com.lorafilm.booking.infrastructure.enums.RetryTaskType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRetryTaskRepository extends JpaRepository<BookingRetryTask, Long> {

    Optional<BookingRetryTask> findByPublicId(String publicId);

    List<BookingRetryTask> findByStatusAndNextRetryAtBefore(RetryTaskStatus status, Instant now, Pageable pageable);

    boolean existsByTaskTypeAndReferenceId(RetryTaskType taskType, Long referenceId);
}

