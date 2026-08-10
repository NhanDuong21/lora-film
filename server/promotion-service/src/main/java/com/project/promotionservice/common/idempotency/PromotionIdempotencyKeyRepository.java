package com.project.promotionservice.common.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;

public interface PromotionIdempotencyKeyRepository extends JpaRepository<PromotionIdempotencyKey, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select keyRecord from PromotionIdempotencyKey keyRecord
            where keyRecord.clientId = :clientId
              and keyRecord.apiName = :apiName
              and keyRecord.idempotencyKey = :idempotencyKey
              and keyRecord.deletedAt is null
            """)
    Optional<PromotionIdempotencyKey> findForUpdate(
            @Param("clientId") String clientId,
            @Param("apiName") String apiName,
            @Param("idempotencyKey") String idempotencyKey);

    @Modifying
    @Query("delete from PromotionIdempotencyKey k where k.expiredAt <= :now")
    int deleteExpired(@Param("now") Instant now);
}
