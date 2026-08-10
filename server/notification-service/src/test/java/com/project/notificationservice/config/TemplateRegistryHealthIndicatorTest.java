package com.project.notificationservice.config;

import com.project.notificationservice.template.TemplateRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TemplateRegistryHealthIndicatorTest {

    @Test
    void omitsUnavailableOptionalDetailsInsteadOfFailingHealthCheck() {
        TemplateRegistry registry = mock(TemplateRegistry.class);
        when(registry.health()).thenReturn(
                new TemplateRegistry.RegistryHealth(false, "JGit", "main", null, null));

        var health = new TemplateRegistryHealthIndicator(registry).health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails())
                .containsEntry("provider", "JGit")
                .containsEntry("branch", "main")
                .doesNotContainKeys("headCommit", "message");
    }
}
