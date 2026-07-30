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
        return builder.withDetail("provider", registryHealth.provider())
                .withDetail("branch", registryHealth.branch())
                .withDetail("headCommit", registryHealth.headCommit())
                .withDetail("message", registryHealth.message())
                .build();
    }
}
