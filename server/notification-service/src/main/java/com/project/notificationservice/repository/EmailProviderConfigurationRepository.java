package com.project.notificationservice.repository;

import com.project.notificationservice.entity.EmailProviderConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailProviderConfigurationRepository
        extends JpaRepository<EmailProviderConfiguration, Long> {

    Optional<EmailProviderConfiguration> findByConfigKey(String configKey);
}
