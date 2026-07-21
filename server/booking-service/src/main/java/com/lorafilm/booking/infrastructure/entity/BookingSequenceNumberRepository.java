package com.lorafilm.booking.infrastructure.entity;

import com.lorafilm.booking.infrastructure.entity.BookingSequenceNumber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface BookingSequenceNumberRepository extends JpaRepository<BookingSequenceNumber, Long> {

    Optional<BookingSequenceNumber> findBySequenceNameAndSequenceDate(String sequenceName, LocalDate sequenceDate);
}
