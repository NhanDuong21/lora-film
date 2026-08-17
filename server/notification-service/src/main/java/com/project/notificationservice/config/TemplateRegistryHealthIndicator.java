package com.project.notificationservice.config;

import com.project.notificationservice.template.TemplateRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("notificationTemplateGit")
public class TemplateRegistryHealthIndicator implements HealthIndicator {

    private final TemplateRegistry registry;

    public TemplateRegistryHealthIndicator(TemplateRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Health health() {
        TemplateRegistry.RegistryHealth registryHealth = registry.health();
        Health.Builder builder = registryHealth.available() ? Health.up() : Health.down();
        addDetail(builder, "provider", registryHealth.provider());
        addDetail(builder, "branch", registryHealth.branch());
        addDetail(builder, "remoteUri", registryHealth.remoteUri());
        addDetail(builder, "repository", registryHealth.repository());
        addDetail(builder, "headCommit", registryHealth.headCommit());
        addDetail(builder, "remoteHeadCommit", registryHealth.remoteHeadCommit());
        if (registryHealth.lastSyncedAt() != null) {
            builder.withDetail("lastSyncedAt", registryHealth.lastSyncedAt());
        }
        addDetail(builder, "message", registryHealth.message());
        return builder.build();
    }

    private void addDetail(Health.Builder builder, String key, String value) {
        if (value != null) {
            builder.withDetail(key, value);
        }
    }
}
