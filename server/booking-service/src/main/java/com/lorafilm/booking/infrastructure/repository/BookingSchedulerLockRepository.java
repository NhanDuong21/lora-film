package com.lorafilm.booking.infrastructure.repository;

import com.lorafilm.booking.infrastructure.entity.BookingSchedulerLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookingSchedulerLockRepository extends JpaRepository<BookingSchedulerLock, Long> {

    Optional<BookingSchedulerLock> findBySchedulerName(String schedulerName);
}
