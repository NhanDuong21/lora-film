package com.project.scoreservice.repository;

import com.project.scoreservice.entity.UserScore;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserScoreRepository extends JpaRepository<UserScore, Long> {

    Optional<UserScore> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UserScore u WHERE u.userId = :userId")
    Optional<UserScore> findWithLockByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE UserScore u SET u.currentPoints = u.currentPoints - :points " +
           "WHERE u.userId = :userId AND u.currentPoints >= :points")
    int deductPointsAtomic(@Param("userId") Long userId, @Param("points") Integer points);

    @Modifying
    @Query("UPDATE UserScore u SET u.currentPoints = u.currentPoints + :points, " +
           "u.accumulatedPoints = u.accumulatedPoints + :points " +
           "WHERE u.userId = :userId")
    int addPointsAtomic(@Param("userId") Long userId, @Param("points") Integer points);

    @Modifying
    @Query("UPDATE UserScore u SET u.currentPoints = u.currentPoints + :points " +
           "WHERE u.userId = :userId")
    int addCurrentPointsOnlyAtomic(@Param("userId") Long userId, @Param("points") Integer points);
}
