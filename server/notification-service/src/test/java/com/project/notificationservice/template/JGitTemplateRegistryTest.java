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
import org.eclipse.jgit.storage.file.WindowCacheConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.junit.jupiter.api.AfterEach;
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

    @AfterEach
    void tearDown() {
        if (registry != null) registry.close();
        // JGit caches pack windows globally; reinstalling releases Windows file handles
        // before JUnit removes the temporary repositories.
        new WindowCacheConfig().install();
    }

    @BeforeEach
    void setUp() throws Exception {
        Path repository = temporaryDirectory.resolve("registry");
        Files.createDirectories(repository);
        try (Git git = Git.init().setInitialBranch("main").setDirectory(repository.toFile()).call()) {
            Files.writeString(repository.resolve("README.md"), "external template registry");
            Path legacy = repository.resolve("email/vi/booking/booking_confirmed.html");
            Files.createDirectories(legacy.getParent());
            Files.writeString(legacy, """
                    <!doctype html>
                    <html><head><title>Booking Confirmed - LoraFilm</title></head>
                    <body><h1>Xin chào {{user_name}}</h1><p>Mã vé {{booking_code}}</p></body></html>
                    """);
            Path englishOnly = repository.resolve("email/en/payment/payment_failed.html");
            Files.createDirectories(englishOnly.getParent());
            Files.writeString(englishOnly, """
                    <!doctype html>
                    <html><head><title>Payment Failed - LoraFilm</title></head>
                    <body><h1>Hello {{user_name}}</h1><p>Payment {{transaction_id}} failed.</p></body></html>
                    """);
            Path archived = repository.resolve("email/_archive/vi/auth/register_otp.html");
            Files.createDirectories(archived.getParent());
            Files.writeString(archived, """
                    <!doctype html>
                    <html><head><title>Archived registration OTP</title></head>
                    <body><p>{{otp_code}}</p></body></html>
                    """);
            git.add().addFilepattern(".").call();
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
                5,
                true);
        registry.initialize();
    }

    @Test
    void ignoresAuxiliaryEmailDirectoriesWhenListingPublishedTemplates() {
        assertThat(registry.findTemplates(null))
                .extracting(TemplateRegistry.TemplateSummary::templateKey)
                .contains("BOOKING_CONFIRMED", "PAYMENT_FAILED")
                .doesNotContain("REGISTER_OTP");
    }

    @Test
    void automaticallyFastForwardsPublishedBranchWhenRemoteChanges() throws Exception {
        registry.close();
        registry = null;

        Path remote = temporaryDirectory.resolve("remote.git");
        try (Git ignored = Git.init()
                .setBare(true)
                .setInitialBranch("main")
                .setDirectory(remote.toFile())
                .call()) {
            // Bare remote used by the publisher and registry clone below.
        }

        Path publisherDirectory = temporaryDirectory.resolve("publisher");
        Path registryDirectory = temporaryDirectory.resolve("remote-registry");
        try (Git publisher = Git.init()
                .setInitialBranch("main")
                .setDirectory(publisherDirectory.toFile())
                .call()) {
            Path template = publisherDirectory.resolve(
                    "email/vi/booking/booking_confirmed.html");
            Files.createDirectories(template.getParent());
            Files.writeString(template, legacyBookingTemplate("Phiên bản một"));
            publisher.add().addFilepattern(".").call();
            publisher.commit().setMessage("Initial template")
                    .setAuthor("Test", "test@example.com").call();
            publisher.remoteAdd()
                    .setName("origin")
                    .setUri(new URIish(remote.toUri().toString()))
                    .call();
            publisher.push()
                    .setRemote("origin")
                    .setRefSpecs(new RefSpec("refs/heads/main:refs/heads/main"))
                    .call();

            registry = createRegistry(
                    registryDirectory, remote.toUri().toString(), true);
            registry.initialize();
            String initialCommit = registry.getPublishedTemplate(
                    "BOOKING_CONFIRMED", Channel.EMAIL, "vi-VN").commitSha();

            Files.writeString(template, legacyBookingTemplate("Phiên bản hai"));
            publisher.add().addFilepattern(".").call();
            publisher.commit().setMessage("Update template")
                    .setAuthor("Test", "test@example.com").call();
            publisher.push()
                    .setRemote("origin")
                    .setRefSpecs(new RefSpec("refs/heads/main:refs/heads/main"))
                    .call();

            registry.refreshFromRemote();

            TemplateRegistry.TemplateDocument refreshed = registry.getPublishedTemplate(
                    "BOOKING_CONFIRMED", Channel.EMAIL, "vi-VN");
            assertThat(refreshed.commitSha()).isNotEqualTo(initialCommit);
            assertThat(refreshed.htmlContent()).contains("Phiên bản hai");
        }
    }

    @Test
    void automaticRefreshDoesNotOverwriteLocalChanges() throws Exception {
        registry.close();
        registry = null;

        Path remote = temporaryDirectory.resolve("dirty-remote.git");
        try (Git ignored = Git.init()
                .setBare(true)
                .setInitialBranch("main")
                .setDirectory(remote.toFile())
                .call()) {
            // Bare remote used by the publisher and registry clone below.
        }

        Path publisherDirectory = temporaryDirectory.resolve("dirty-publisher");
        Path registryDirectory = temporaryDirectory.resolve("dirty-registry");
        try (Git publisher = Git.init()
                .setInitialBranch("main")
                .setDirectory(publisherDirectory.toFile())
                .call()) {
            Path publisherTemplate = publisherDirectory.resolve(
                    "email/vi/booking/booking_confirmed.html");
            Files.createDirectories(publisherTemplate.getParent());
            Files.writeString(publisherTemplate, legacyBookingTemplate("Bản gốc"));
            publisher.add().addFilepattern(".").call();
            publisher.commit().setMessage("Initial template")
                    .setAuthor("Test", "test@example.com").call();
            publisher.remoteAdd()
                    .setName("origin")
                    .setUri(new URIish(remote.toUri().toString()))
                    .call();
            publisher.push()
                    .setRemote("origin")
                    .setRefSpecs(new RefSpec("refs/heads/main:refs/heads/main"))
                    .call();

            registry = createRegistry(
                    registryDirectory, remote.toUri().toString(), true);
            registry.initialize();

            Path localTemplate = registryDirectory.resolve(
                    "email/vi/booking/booking_confirmed.html");
            Files.writeString(localTemplate, legacyBookingTemplate("Thay đổi local"));
            Files.writeString(publisherTemplate, legacyBookingTemplate("Thay đổi remote"));
            publisher.add().addFilepattern(".").call();
            publisher.commit().setMessage("Remote update")
                    .setAuthor("Test", "test@example.com").call();
            publisher.push()
                    .setRemote("origin")
                    .setRefSpecs(new RefSpec("refs/heads/main:refs/heads/main"))
                    .call();

            registry.refreshFromRemote();

            assertThat(Files.readString(localTemplate)).contains("Thay đổi local");
            assertThat(registry.health().message()).contains("local changes");
        }
    }

    @Test
    void draftCannotBeUsedForPublishedDelivery() {
        registry.createDraft(new CreateTemplateDraftCommand("NEW_TEMPLATE", "admin", content("one")));

        assertThatThrownBy(() -> registry.getPublishedTemplate(
                "NEW_TEMPLATE", Channel.EMAIL, "vi-VN"))
                .isInstanceOf(NotificationException.class)
                .extracting(exception -> ((NotificationException) exception).getErrorCode())
                .isEqualTo("TEMPLATE_NOT_FOUND");
    }

    @Test
    void pushesDraftCreateAndUpdateThenRemovesRemoteBranchAfterPublish() throws Exception {
        registry.close();
        registry = null;

        Path source = temporaryDirectory.resolve("registry");
        Path remote = temporaryDirectory.resolve("draft-remote.git");
        try (Git bare = Git.init().setBare(true).setDirectory(remote.toFile()).call();
             Git publisher = Git.open(source.toFile())) {
            publisher.remoteAdd()
                    .setName("origin")
                    .setUri(new URIish(remote.toUri().toString()))
                    .call();
            publisher.push()
                    .setRemote("origin")
                    .setRefSpecs(new RefSpec("refs/heads/main:refs/heads/main"))
                    .call();
        }

        Path workingDirectory = temporaryDirectory.resolve("draft-registry");
        registry = createRegistry(workingDirectory, remote.toUri().toString(), true);
        registry.initialize();

        TemplateDraft created = registry.createDraft(
                new CreateTemplateDraftCommand("REMOTE_DRAFT", "admin", content("one")));
        assertThat(remoteHead(remote, created.branch())).isEqualTo(created.commitSha());

        TemplateDraft updated = registry.updateDraft(
                "REMOTE_DRAFT",
                created.draftId(),
                created.commitSha(),
                new TemplateRegistry.UpdateTemplateDraftCommand("Update remote draft", content("two")));
        assertThat(remoteHead(remote, updated.branch())).isEqualTo(updated.commitSha());

        TemplateRegistry.TemplatePublicationResult published = registry.publishDraft(
                "REMOTE_DRAFT", updated.draftId(), updated.commitSha(), "admin");
        assertThat(remoteHead(remote, "main")).isEqualTo(published.commitSha());
        assertThat(remoteHead(remote, updated.branch())).isNull();

        TemplateDraft discarded = registry.createDraft(
                new CreateTemplateDraftCommand("DISCARDED_DRAFT", "admin", content("discard")));
        assertThat(remoteHead(remote, discarded.branch())).isEqualTo(discarded.commitSha());
        registry.deleteDraft("DISCARDED_DRAFT", discarded.draftId(), "admin");
        assertThat(remoteHead(remote, discarded.branch())).isNull();
    }

    @Test
    void readsGitMailRepositoryLayoutByFileName() {
        TemplateRegistry.TemplateDocument document = registry.getPublishedTemplate(
                "BOOKING_CONFIRMED", Channel.EMAIL, "vi-VN");

        assertThat(document.status())
                .isEqualTo(com.project.notificationservice.domain.NotificationTypes.TemplateStatus.PUBLISHED);
        assertThat(document.subject()).isEqualTo("Booking Confirmed - LoraFilm");
        assertThat(document.htmlContent()).contains("{{user_name}}", "{{booking_code}}");
        assertThat(document.variablesSchema()).containsKeys("user_name", "booking_code");
        assertThat(document.commitSha()).matches("[a-f0-9]{40}");
    }

    @Test
    void derivesCompactInAppContentFromTheSameGitHtml() {
        TemplateRegistry.TemplateDocument document = registry.getPublishedTemplate(
                "BOOKING_CONFIRMED", Channel.IN_APP, "vi-VN");

        assertThat(document.subject()).isEqualTo("Booking Confirmed - LoraFilm");
        assertThat(document.textContent()).contains(
                "Xin chào {{user_name}}", "Mã vé {{booking_code}}");
        assertThat(document.commitSha()).matches("[a-f0-9]{40}");
    }

    @Test
    void fallsBackToTheLocaleThatExistsInTheGitRepository() {
        TemplateRegistry.TemplateDocument document = registry.getPublishedTemplate(
                "PAYMENT_FAILED", Channel.IN_APP, "vi-VN");

        assertThat(document.locale()).isEqualTo("vi-VN");
        assertThat(document.subject()).isEqualTo("Payment Failed - LoraFilm");
        assertThat(document.textContent()).contains("{{transaction_id}}");
    }

    @Test
    void listsLegacyTemplatesAlongsideNativeTemplates() {
        assertThat(registry.findTemplates(null))
                .extracting(TemplateRegistry.TemplateSummary::templateKey)
                .contains("BOOKING_CONFIRMED");
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

    private JGitTemplateRegistry createRegistry(
            Path workingDirectory, String remoteUri, boolean autoRefreshEnabled) {
        NotificationAuditLogRepository auditRepository = mock(NotificationAuditLogRepository.class);
        when(auditRepository.save(any(NotificationAuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        @SuppressWarnings("unchecked")
        ObjectProvider<StringRedisTemplate> redisProvider = mock(ObjectProvider.class);
        when(redisProvider.getIfAvailable()).thenReturn(null);
        return new JGitTemplateRegistry(
                new ObjectMapper().findAndRegisterModules(),
                new SafeTemplateRenderer(20_000, 200, 480),
                auditRepository,
                redisProvider,
                workingDirectory.toString(),
                remoteUri,
                "main",
                "",
                "",
                "Test",
                "test@example.com",
                20_000,
                5,
                autoRefreshEnabled);
    }

    private String remoteHead(Path remote, String branch) throws Exception {
        try (Git git = Git.open(remote.toFile())) {
            var ref = git.getRepository().exactRef("refs/heads/" + branch);
            return ref == null ? null : ref.getObjectId().getName();
        }
    }

    private String legacyBookingTemplate(String marker) {
        return """
                <!doctype html>
                <html><head><title>Booking Confirmed - LoraFilm</title></head>
                <body><h1>Xin chào {{user_name}}</h1>
                <p>Mã vé {{booking_code}} - %s</p></body></html>
                """.formatted(marker);
    }
}
