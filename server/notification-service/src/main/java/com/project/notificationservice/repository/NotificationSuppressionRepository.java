package com.project.notificationservice.repository;

import com.project.notificationservice.domain.NotificationTypes.Channel;
import com.project.notificationservice.entity.NotificationSuppression;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface NotificationSuppressionRepository
        extends JpaRepository<NotificationSuppression, Long> {

    Optional<NotificationSuppression> findByDestinationHashAndChannel(
            String destinationHash, Channel channel);

    @Query("""
            select suppression from NotificationSuppression suppression
            where suppression.destinationHash = :destinationHash
              and suppression.channel = :channel
              and (suppression.expiresAt is null or suppression.expiresAt > :now)
            """)
    Optional<NotificationSuppression> findActive(
            @Param("destinationHash") String destinationHash,
            @Param("channel") Channel channel,
            @Param("now") Instant now);
}
