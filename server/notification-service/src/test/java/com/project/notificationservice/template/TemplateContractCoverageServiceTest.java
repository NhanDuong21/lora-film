package com.project.notificationservice.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.notificationservice.domain.NotificationTypes.Category;
import com.project.notificationservice.domain.NotificationTypes.Channel;
import com.project.notificationservice.domain.NotificationTypes.TemplateStatus;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TemplateContractCoverageServiceTest {

    @Test
    void blocksARequiredProducerContractWhenItsActiveTemplateIsMissing() throws Exception {
        TemplateRegistry registry = mock(TemplateRegistry.class);
        when(registry.health()).thenReturn(new TemplateRegistry.RegistryHealth(
                true, "JGit", "main", "https://github.com/NhanDuong21/template-mail.git",
                "NhanDuong21/template-mail", "active", "remote", Instant.now(), "ready"));
        when(registry.findTemplates(null)).thenReturn(List.of(
                summary("FORGOT_PASSWORD_OTP"),
                summary("CHANGE_EMAIL_OTP"),
                summary("BOOKING_CONFIRMED"),
                summary("VOUCHER_GRANTED")));
        TemplateContractCoverageService service = new TemplateContractCoverageService(
                registry, new ObjectMapper(),
                new ClassPathResource("notification-template-contracts.json"));
        service.loadContracts();

        TemplateContractCoverageService.CoverageReport report = service.inspect();

        assertThat(report.blockedRequirements()).isEqualTo(1);
        assertThat(report.items())
                .filteredOn(item -> item.templateKey().equals("REGISTER_OTP"))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.readiness())
                            .isEqualTo(TemplateContractCoverageService.Readiness.BLOCKED);
                    assertThat(item.issueCode()).isEqualTo("REQUIRED_TEMPLATE_MISSING");
                    assertThat(item.sourceService()).isEqualTo("auth-service");
                });
    }

    private TemplateRegistry.TemplateSummary summary(String templateKey) {
        return new TemplateRegistry.TemplateSummary(
                templateKey, templateKey, Category.TRANSACTIONAL, Channel.EMAIL,
                "vi-VN", TemplateStatus.PUBLISHED, null, "active", Instant.now());
    }
}
