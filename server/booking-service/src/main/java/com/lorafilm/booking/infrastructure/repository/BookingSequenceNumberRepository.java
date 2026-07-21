package com.lorafilm.booking.infrastructure.repository;

import com.lorafilm.booking.infrastructure.entity.BookingSequenceNumber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface BookingSequenceNumberRepository extends JpaRepository<BookingSequenceNumber, Long> {

    Optional<BookingSequenceNumber> findBySequenceNameAndSequenceDate(String sequenceName, LocalDate sequenceDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select sequence from BookingSequenceNumber sequence
            where sequence.sequenceName = :sequenceName and sequence.sequenceDate = :sequenceDate
            """)
    Optional<BookingSequenceNumber> findForUpdate(
            @Param("sequenceName") String sequenceName,
            @Param("sequenceDate") LocalDate sequenceDate);
}
