package com.project.notificationservice.repository;

import com.project.notificationservice.entity.WebPushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WebPushSubscriptionRepository extends JpaRepository<WebPushSubscription, Long> {

    Optional<WebPushSubscription> findByPublicId(String publicId);

    List<WebPushSubscription> findByUserPublicIdAndActiveTrueOrderByCreatedAtDesc(
            String userPublicId);
}
