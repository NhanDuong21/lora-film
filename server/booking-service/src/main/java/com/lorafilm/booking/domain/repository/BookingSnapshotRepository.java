package com.lorafilm.booking.domain.repository;

import com.lorafilm.booking.domain.entity.BookingSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingSnapshotRepository extends JpaRepository<BookingSnapshot, Long>, JpaSpecificationExecutor<BookingSnapshot> {
    Optional<BookingSnapshot> findByPublicId(UUID publicId);
}
