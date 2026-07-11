package com.project.notificationservice.repository;

import com.project.notificationservice.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Integer>, JpaSpecificationExecutor<NotificationTemplate> {

    boolean existsByTemplateCode(String templateCode);

    Optional<NotificationTemplate> findByTemplateCode(String templateCode);
}
