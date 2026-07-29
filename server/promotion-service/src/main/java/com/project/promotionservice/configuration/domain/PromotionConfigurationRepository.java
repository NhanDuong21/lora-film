package com.project.promotionservice.configuration.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface PromotionConfigurationRepository extends
        JpaRepository<PromotionConfiguration, Long>,
        JpaSpecificationExecutor<PromotionConfiguration> {

    Optional<PromotionConfiguration> findByPublicIdAndDeletedAtIsNull(String publicId);
    Optional<PromotionConfiguration> findByConfigKeyAndDeletedAtIsNull(String configKey);
    List<PromotionConfiguration> findByStatusAndDeletedAtIsNull(ConfigurationStatus status);
    Page<PromotionConfiguration> findByDeletedAtIsNull(Pageable pageable);
    boolean existsByConfigKey(String configKey);
    boolean existsByConfigKeyAndDeletedAtIsNull(String configKey);
}
