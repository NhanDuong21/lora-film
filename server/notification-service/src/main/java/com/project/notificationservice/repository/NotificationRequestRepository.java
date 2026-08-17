package com.project.notificationservice.repository;

import com.project.notificationservice.entity.NotificationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.Optional;

public interface NotificationRequestRepository
        extends JpaRepository<NotificationRequest, Long>, JpaSpecificationExecutor<NotificationRequest> {

    Optional<NotificationRequest> findByPublicId(String publicId);

    Optional<NotificationRequest> findByIdempotencyKey(String idempotencyKey);

    long countByCreatedAtGreaterThanEqual(Instant since);

    long countByTestFalseAndCreatedAtGreaterThanEqual(Instant since);
}
