package com.project.scoreservice.repository;

import com.project.scoreservice.entity.PointExpirationBucket;
import com.project.scoreservice.enumtype.PointExpirationBucketStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PointExpirationBucketRepository extends JpaRepository<PointExpirationBucket, Long> {

    Optional<PointExpirationBucket> findByScoreHistory_Id(Long historyId);

    List<PointExpirationBucket> findByUserScore_UserIdOrderByExpirationDateAsc(Long userId);

    List<PointExpirationBucket> findByUserScore_UserIdAndStatusInOrderByExpirationDateAsc(Long userId, List<PointExpirationBucketStatus> statuses);

    List<PointExpirationBucket> findByStatusInAndExpirationDateBefore(List<PointExpirationBucketStatus> statuses, LocalDate date);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM PointExpirationBucket b WHERE b.userScore.userId = :userId AND b.status IN :statuses ORDER BY b.expirationDate ASC")
    List<PointExpirationBucket> findWithLockByUserIdAndStatusIn(@Param("userId") Long userId, @Param("statuses") List<PointExpirationBucketStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM PointExpirationBucket b WHERE b.scoreHistory.id = :historyId")
    Optional<PointExpirationBucket> findWithLockByHistoryId(@Param("historyId") Long historyId);
}
