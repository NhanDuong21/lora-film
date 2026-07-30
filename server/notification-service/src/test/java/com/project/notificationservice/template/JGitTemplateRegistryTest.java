package com.project.notificationservice.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.notificationservice.domain.NotificationTypes.Category;
import com.project.notificationservice.domain.NotificationTypes.Channel;
import com.project.notificationservice.entity.NotificationAuditLog;
import com.project.notificationservice.exception.NotificationException;
import com.project.notificationservice.repository.NotificationAuditLogRepository;
import com.project.notificationservice.template.TemplateRegistry.CreateTemplateDraftCommand;
import com.project.notificationservice.template.TemplateRegistry.TemplateContent;
import com.project.notificationservice.template.TemplateRegistry.TemplateDraft;
import com.project.notificationservice.template.TemplateRegistry.VariableDefinition;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JGitTemplateRegistryTest {

    @TempDir
    Path temporaryDirectory;

    private JGitTemplateRegistry registry;

    @BeforeEach
    void setUp() throws Exception {
        Path repository = temporaryDirectory.resolve("registry");
        Files.createDirectories(repository);
        try (Git git = Git.init().setInitialBranch("main").setDirectory(repository.toFile()).call()) {
            Files.writeString(repository.resolve("README.md"), "external template registry");
            git.add().addFilepattern("README.md").call();
            git.commit().setMessage("Initialize registry")
                    .setAuthor("Test", "test@example.com").call();
        }
        NotificationAuditLogRepository auditRepository = mock(NotificationAuditLogRepository.class);
        when(auditRepository.save(any(NotificationAuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        @SuppressWarnings("unchecked")
        ObjectProvider<StringRedisTemplate> redisProvider = mock(ObjectProvider.class);
        when(redisProvider.getIfAvailable()).thenReturn(null);
        registry = new JGitTemplateRegistry(
                new ObjectMapper().findAndRegisterModules(),
                new SafeTemplateRenderer(20_000, 200, 480),
                auditRepository,
                redisProvider,
                repository.toString(),
                "",
                "main",
                "",
                "",
                "Test",
                "test@example.com",
                20_000,
                5);
        registry.initialize();
    }

    @Test
    void draftCannotBeUsedForPublishedDelivery() {
        registry.createDraft(new CreateTemplateDraftCommand("TICKET_PURCHASED", "admin", content("one")));

        assertThatThrownBy(() -> registry.getPublishedTemplate(
                "TICKET_PURCHASED", Channel.EMAIL, "vi-VN"))
                .isInstanceOf(NotificationException.class)
                .extracting(exception -> ((NotificationException) exception).getErrorCode())
                .isEqualTo("TEMPLATE_NOT_FOUND");
    }

    @Test
    void publishCreatesCommitAndVersionTag() {
        TemplateDraft draft = registry.createDraft(
                new CreateTemplateDraftCommand("TICKET_PURCHASED", "admin", content("one")));

        TemplateRegistry.TemplatePublicationResult published = registry.publishDraft(
                "TICKET_PURCHASED", draft.draftId(), draft.commitSha(), "admin");

        assertThat(published.version()).isEqualTo("v000001");
        assertThat(published.gitTag()).endsWith("/v000001");
        assertThat(published.commitSha()).matches("[a-f0-9]{40}");
        assertThat(registry.getPublishedTemplate(
                "TICKET_PURCHASED", Channel.EMAIL, "vi-VN").status())
                .isEqualTo(com.project.notificationservice.domain.NotificationTypes.TemplateStatus.PUBLISHED);
    }

    @Test
    void staleDraftCommitReturnsConflict() {
        TemplateDraft draft = registry.createDraft(
                new CreateTemplateDraftCommand("TICKET_PURCHASED", "admin", content("one")));

        assertThatThrownBy(() -> registry.updateDraft(
                "TICKET_PURCHASED", draft.draftId(),
                "0000000000000000000000000000000000000000",
                new TemplateRegistry.UpdateTemplateDraftCommand("stale", content("two"))))
                .isInstanceOf(NotificationException.class)
                .extracting(exception -> ((NotificationException) exception).getStatus().value())
                .isEqualTo(409);
    }

    @Test
    void rollbackCreatesNewCommitAndNeverRewritesHistory() {
        TemplateDraft first = registry.createDraft(
                new CreateTemplateDraftCommand("TICKET_PURCHASED", "admin", content("one")));
        registry.publishDraft("TICKET_PURCHASED", first.draftId(), first.commitSha(), "admin");
        TemplateDraft second = registry.createDraft(
                new CreateTemplateDraftCommand("TICKET_PURCHASED", "admin", content("two")));
        registry.publishDraft("TICKET_PURCHASED", second.draftId(), second.commitSha(), "admin");

        TemplateRegistry.TemplatePublicationResult rollback = registry.rollback(
                "TICKET_PURCHASED", Channel.EMAIL, "vi-VN", "v000001", "admin");

        assertThat(rollback.rollback()).isTrue();
        assertThat(rollback.version()).isEqualTo("v000003");
        assertThat(registry.findVersions("TICKET_PURCHASED", Channel.EMAIL, "vi-VN"))
                .extracting(TemplateRegistry.TemplateVersionSummary::version)
                .containsExactly("v000003", "v000002", "v000001");
    }

    private TemplateContent content(String marker) {
        return new TemplateContent(
                "Test " + marker,
                "Test",
                Category.TRANSACTIONAL,
                Channel.EMAIL,
                "vi-VN",
                Map.of("customerName", new VariableDefinition("string", true)),
                Map.of("customerName", "A"),
                "{{customerName}}",
                "<p>{{customerName}}</p>",
                "{{customerName}}");
    }
}
