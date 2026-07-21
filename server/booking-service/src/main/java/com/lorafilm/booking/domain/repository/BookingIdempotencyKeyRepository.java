package com.lorafilm.booking.domain.repository;

import com.lorafilm.booking.domain.entity.BookingIdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookingIdempotencyKeyRepository extends JpaRepository<BookingIdempotencyKey, Long>, JpaSpecificationExecutor<BookingIdempotencyKey> {
    Optional<BookingIdempotencyKey> findByIdempotencyKey(String idempotencyKey);
}
