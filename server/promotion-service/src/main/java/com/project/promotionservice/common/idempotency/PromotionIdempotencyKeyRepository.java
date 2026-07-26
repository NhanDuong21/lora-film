package com.project.promotionservice.common.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PromotionIdempotencyKeyRepository extends JpaRepository<PromotionIdempotencyKey, Long> {

    Optional<PromotionIdempotencyKey> findByIdempotencyKey(String idempotencyKey);
}
