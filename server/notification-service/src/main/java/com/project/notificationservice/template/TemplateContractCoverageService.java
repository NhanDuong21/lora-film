package com.project.notificationservice.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.notificationservice.domain.NotificationTypes.Channel;
import com.project.notificationservice.domain.NotificationTypes.TemplateStatus;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class TemplateContractCoverageService {

    private final TemplateRegistry registry;
    private final ObjectMapper objectMapper;
    private final Resource contractResource;
    private volatile List<ProducerContract> contracts = List.of();

    public TemplateContractCoverageService(
            TemplateRegistry registry,
            ObjectMapper objectMapper,
            @Value("classpath:notification-template-contracts.json") Resource contractResource) {
        this.registry = registry;
        this.objectMapper = objectMapper;
        this.contractResource = contractResource;
    }

    @PostConstruct
    public void loadContracts() throws IOException {
        try (var input = contractResource.getInputStream()) {
            ContractConfiguration configuration = objectMapper.readValue(
                    input, ContractConfiguration.class);
            contracts = configuration.contracts() == null
                    ? List.of()
                    : List.copyOf(configuration.contracts());
        }
    }

    public CoverageReport inspect() {
        TemplateRegistry.RegistryHealth health = registry.health();
        List<TemplateRegistry.TemplateSummary> templates;
        try {
            templates = health.available() ? registry.findTemplates(null) : List.of();
        } catch (RuntimeException exception) {
            health = new TemplateRegistry.RegistryHealth(
                    false, health.provider(), health.branch(), health.remoteUri(),
                    health.repository(), health.headCommit(), health.remoteHeadCommit(),
                    health.lastSyncedAt(), exception.getMessage());
            templates = List.of();
        }

        Set<String> activeVariants = new HashSet<>();
        templates.stream()
                .filter(template -> template.status() == TemplateStatus.PUBLISHED)
                .forEach(template -> activeVariants.add(identity(
                        template.templateKey(), template.channel(), template.locale())));

        List<CoverageItem> items = new ArrayList<>();
        for (ProducerContract contract : contracts) {
            for (String locale : contract.locales()) {
                TemplateRegistry.TemplateSummary matched = templates.stream()
                        .filter(template -> contract.templateKey().equals(template.templateKey()))
                        .filter(template -> template.channel() == Channel.EMAIL)
                        .filter(template -> locale.equals(template.locale()))
                        .findFirst()
                        .orElse(null);
                boolean available = health.available()
                        && activeVariants.contains(identity(contract.templateKey(), Channel.EMAIL, locale));
                Readiness readiness = available
                        ? Readiness.READY
                        : contract.required() ? Readiness.BLOCKED : Readiness.WARNING;
                String issueCode = available ? null
                        : health.available() ? "REQUIRED_TEMPLATE_MISSING" : "REGISTRY_UNAVAILABLE";
                String message = available
                        ? "Contract đã có template đang hoạt động."
                        : health.available()
                                ? contract.templateKey() + " · EMAIL · " + locale
                                        + " không có template đang hoạt động."
                                : "Nguồn template hiện không khả dụng nên chưa thể kiểm tra contract.";
                items.add(new CoverageItem(
                        contract.templateKey(), contract.displayName(), contract.sourceService(),
                        contract.eventTypes(), contract.channels(), locale, contract.required(),
                        readiness, issueCode, message,
                        matched == null ? null : matched.commitSha()));
            }
        }

        long ready = items.stream().filter(item -> item.readiness() == Readiness.READY).count();
        long warnings = items.stream().filter(item -> item.readiness() == Readiness.WARNING).count();
        long blocked = items.stream().filter(item -> item.readiness() == Readiness.BLOCKED).count();
        return new CoverageReport(
                items.size(), ready, warnings, blocked,
                activeVariants.size(), List.copyOf(items), Instant.now());
    }

    public List<ProducerContract> contracts() {
        return contracts;
    }

    private String identity(String templateKey, Channel channel, String locale) {
        return templateKey + "|" + channel.name() + "|" + locale;
    }

    public enum Readiness {
        READY,
        WARNING,
        BLOCKED
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContractConfiguration(List<ProducerContract> contracts) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProducerContract(
            String templateKey,
            String displayName,
            String sourceService,
            List<String> eventTypes,
            List<Channel> channels,
            List<String> locales,
            boolean required) {
    }

    public record CoverageItem(
            String templateKey,
            String displayName,
            String sourceService,
            List<String> eventTypes,
            List<Channel> channels,
            String locale,
            boolean required,
            Readiness readiness,
            String issueCode,
            String message,
            String activeRevision) {
    }

    public record CoverageReport(
            long totalRequirements,
            long readyRequirements,
            long warningRequirements,
            long blockedRequirements,
            long activeTemplateVariants,
            List<CoverageItem> items,
            Instant checkedAt) {
    }
}
