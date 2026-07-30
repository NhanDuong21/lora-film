package com.project.notificationservice.repository;

import com.project.notificationservice.entity.InAppNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface InAppNotificationRepository extends JpaRepository<InAppNotification, Long> {

    Page<InAppNotification> findByUserPublicIdAndExpiresAtAfterOrUserPublicIdAndExpiresAtIsNullOrderByCreatedAtDesc(
            String activeUserPublicId, Instant now, String nonExpiringUserPublicId, Pageable pageable);

    Optional<InAppNotification> findByPublicIdAndUserPublicId(String publicId, String userPublicId);

    @Query("""
            select count(item) from InAppNotification item
            where item.userPublicId = :user
              and item.readAt is null
              and (item.expiresAt is null or item.expiresAt > :now)
            """)
    long countActiveUnread(
            @Param("user") String userPublicId,
            @Param("now") Instant now);

    @Modifying
    @Query("""
            update InAppNotification item set item.readAt = :now
            where item.userPublicId = :user
              and item.readAt is null
              and (item.expiresAt is null or item.expiresAt > :now)
            """)
    int markAllRead(@Param("user") String userPublicId, @Param("now") Instant now);
}
