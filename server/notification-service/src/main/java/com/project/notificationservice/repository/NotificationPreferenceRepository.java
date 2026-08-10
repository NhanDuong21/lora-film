package com.project.notificationservice.repository;

import com.project.notificationservice.domain.NotificationTypes.Channel;
import com.project.notificationservice.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    Optional<NotificationPreference> findByUserPublicIdAndChannelAndCategory(
            String userPublicId, Channel channel, String category);
}
