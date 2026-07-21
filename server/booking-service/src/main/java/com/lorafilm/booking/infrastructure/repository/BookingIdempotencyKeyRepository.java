package com.lorafilm.booking.infrastructure.repository;

import com.lorafilm.booking.infrastructure.entity.BookingIdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookingIdempotencyKeyRepository extends JpaRepository<BookingIdempotencyKey, Long> {

    Optional<BookingIdempotencyKey> findByIdempotencyKey(String idempotencyKey);
}
