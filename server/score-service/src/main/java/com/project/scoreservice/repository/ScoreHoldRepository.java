package com.project.scoreservice.repository;

import com.project.scoreservice.entity.ScoreHold;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScoreHoldRepository extends JpaRepository<ScoreHold, Long> {

    Optional<ScoreHold> findByBookingId(Long bookingId);

    Optional<ScoreHold> findByHoldCode(String holdCode);

    Optional<ScoreHold> findByIdempotencyKey(String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM ScoreHold h WHERE h.bookingId = :bookingId")
    Optional<ScoreHold> findWithLockByBookingId(@Param("bookingId") Long bookingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM ScoreHold h WHERE h.holdCode = :holdCode")
    Optional<ScoreHold> findWithLockByHoldCode(@Param("holdCode") String holdCode);

    @Query("SELECT COALESCE(SUM(h.points), 0) FROM ScoreHold h WHERE h.userScore.userId = :userId AND h.status = com.project.scoreservice.enumtype.ScoreHoldStatus.ACTIVE AND h.expiredAt > CURRENT_TIMESTAMP")
    Integer sumActiveHeldPointsByUserId(@Param("userId") Long userId);
}
