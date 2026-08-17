package com.project.notificationservice.template;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.notificationservice.domain.NotificationTypes.Category;
import com.project.notificationservice.domain.NotificationTypes.Channel;
import com.project.notificationservice.domain.NotificationTypes.TemplateStatus;
import com.project.notificationservice.entity.NotificationAuditLog;
import com.project.notificationservice.exception.NotificationException;
import com.project.notificationservice.repository.NotificationAuditLogRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Component
public class JGitTemplateRegistry implements TemplateRegistry {

    private static final Logger log = LoggerFactory.getLogger(JGitTemplateRegistry.class);
    private static final String MANIFEST = "manifest.json";
    private static final String SUBJECT = "subject.hbs";
    private static final String HTML = "content.html.hbs";
    private static final String TEXT = "content.txt.hbs";
    private static final Pattern LEGACY_EXPRESSION = Pattern
            .compile("\\{\\{\\s*#?(?:each|if|unless)?\\s*([A-Za-z_][A-Za-z0-9_.]*)\\s*}}");
    private static final Pattern HTML_TITLE = Pattern.compile("(?is)<title[^>]*>(.*?)</title>");
    private static final Pattern FIRST_HEADING = Pattern.compile("(?is)<h1[^>]*>(.*?)</h1>");
    private static final Pattern FIRST_PARAGRAPH = Pattern.compile("(?is)<p[^>]*>(.*?)</p>");
    private static final Map<String, String> LEGACY_TEMPLATE_ALIASES = Map.of(
            "TICKET_PURCHASED", "booking/booking_confirmed.html");

    private final ObjectMapper objectMapper;
    private final SafeTemplateRenderer renderer;
    private final NotificationAuditLogRepository auditRepository;
    private final ObjectProvider<StringRedisTemplate> redisProvider;
    private final Path workingDirectory;
    private final String remoteUri;
    private final String publishedBranch;
    private final String username;
    private final String accessToken;
    private final String authorName;
    private final String authorEmail;
    private final int maxTemplateBytes;
    private final int fetchTimeoutSeconds;
    private final boolean autoRefreshEnabled;
    private final ReentrantLock lock = new ReentrantLock(true);

    private volatile Git git;
    private volatile String initializationError;
    private volatile String lastRefreshError;

    public JGitTemplateRegistry(
            ObjectMapper objectMapper,
            SafeTemplateRenderer renderer,
            NotificationAuditLogRepository auditRepository,
            ObjectProvider<StringRedisTemplate> redisProvider,
            @Value("${notification.git.working-directory}") String workingDirectory,
            @Value("${notification.git.remote-uri:}") String remoteUri,
            @Value("${notification.git.published-branch:main}") String publishedBranch,
            @Value("${notification.git.username:}") String username,
            @Value("${notification.git.access-token:}") String accessToken,
            @Value("${notification.git.author-name:LoraFilm Notification Service}") String authorName,
            @Value("${notification.git.author-email:notifications@lorafilm.local}") String authorEmail,
            @Value("${notification.git.max-template-bytes:200000}") int maxTemplateBytes,
            @Value("${notification.git.fetch-timeout-seconds:15}") int fetchTimeoutSeconds,
            @Value("${notification.git.auto-refresh-enabled:true}") boolean autoRefreshEnabled) {
        this.objectMapper = objectMapper;
        this.renderer = renderer;
        this.auditRepository = auditRepository;
        this.redisProvider = redisProvider;
        this.workingDirectory = Path.of(workingDirectory).toAbsolutePath().normalize();
        this.remoteUri = remoteUri;
        this.publishedBranch = requireGitName(publishedBranch, "published branch");
        this.username = username;
        this.accessToken = accessToken;
        this.authorName = authorName;
        this.authorEmail = authorEmail;
        this.maxTemplateBytes = maxTemplateBytes;
        this.fetchTimeoutSeconds = fetchTimeoutSeconds;
        this.autoRefreshEnabled = autoRefreshEnabled;
    }

    @PostConstruct
    public void initialize() {
        lock.lock();
        try {
            if (Files.isDirectory(workingDirectory.resolve(".git"), LinkOption.NOFOLLOW_LINKS)) {
                git = Git.open(workingDirectory.toFile());
                fetch();
                checkoutPublished();
                initializationError = null;
                return;
            }
            if (remoteUri == null || remoteUri.isBlank()) {
                initializationError = "NOTIFICATION_TEMPLATE_GIT_URI is not configured";
                return;
            }
            Files.createDirectories(workingDirectory.getParent());
            try {
                git = Git.cloneRepository()
                        .setURI(remoteUri)
                        .setDirectory(workingDirectory.toFile())
                        .setBranch(publishedBranch)
                        .setCredentialsProvider(credentials())
                        .setTimeout(fetchTimeoutSeconds)
                        .call();
            } catch (org.eclipse.jgit.api.errors.TransportException ex) {
                log.warn("Git clone with configured credentials failed ({}), falling back to unauthenticated clone...",
                        ex.getMessage());
                git = Git.cloneRepository()
                        .setURI(remoteUri)
                        .setDirectory(workingDirectory.toFile())
                        .setBranch(publishedBranch)
                        .setCredentialsProvider(null)
                        .setTimeout(fetchTimeoutSeconds)
                        .call();
            }
            initializationError = null;
        } catch (Exception exception) {
            initializationError = safeMessage(exception);
            closeGit();
        } finally {
            lock.unlock();
        }
    }

    @PreDestroy
    public void close() {
        lock.lock();
        try {
            closeGit();
        } finally {
            lock.unlock();
        }
    }

    @Scheduled(fixedDelayString = "${notification.git.refresh-interval-ms:30000}", initialDelayString = "${notification.git.refresh-initial-delay-ms:5000}")
    public void refreshFromRemote() {
        if (!autoRefreshEnabled || remoteUri == null || remoteUri.isBlank() || !lock.tryLock()) {
            return;
        }
        try {
            String updatedCommit = fastForwardPublishedBranch();
            lastRefreshError = null;
            if (updatedCommit != null) {
                log.info("Template registry fast-forwarded {}/{} to commit {}",
                        remoteUri, publishedBranch, updatedCommit);
            }
        } catch (Exception exception) {
            lastRefreshError = safeMessage(exception);
            log.warn("Unable to refresh template registry from {}/{}: {}",
                    remoteUri, publishedBranch, lastRefreshError);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public TemplateDraft createDraft(CreateTemplateDraftCommand command) {
        requireCommand(command.templateKey(), command.actorPublicId(), command.content());
        lock.lock();
        try {
            Git active = requireGit();
            fetch();
            checkoutPublished();
            String baseSha = head(active);
            String draftId = UUID.randomUUID().toString();
            String branch = "draft/" + command.templateKey().toLowerCase(Locale.ROOT)
                    + "/" + safeSegment(command.actorPublicId()) + "/" + draftId;
            active.checkout().setCreateBranch(true).setName(branch).call();
            writeContent(command.templateKey(), command.content(), TemplateStatus.DRAFT, baseSha);
            RevCommit commit = commit("Create draft " + command.templateKey());
            pushBranch(branch);
            TemplateDocument document = readDocument(command.templateKey(), command.content().channel(),
                    command.content().locale(), commit.getName(), null);
            return new TemplateDraft(command.templateKey(), draftId, branch, baseSha, commit.getName(), document);
        } catch (NotificationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw registryFailure("Unable to create template draft", exception);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public TemplateDraft updateDraft(
            String templateKey,
            String draftId,
            String expectedCommitSha,
            UpdateTemplateDraftCommand command) {
        lock.lock();
        try {
            String branch = findDraftBranch(templateKey, draftId);
            checkout(branch);
            assertExpectedHead(expectedCommitSha);
            Manifest previous = readManifest(findTemplateDirectory(templateKey, command.content().channel(),
                    command.content().locale()));
            writeContent(templateKey, command.content(), TemplateStatus.DRAFT, previous.baseCommitSha());
            RevCommit commit = commit(command.changeSummary() == null || command.changeSummary().isBlank()
                    ? "Update draft " + templateKey
                    : command.changeSummary());
            pushBranch(branch);
            TemplateDocument document = readDocument(templateKey, command.content().channel(),
                    command.content().locale(), commit.getName(), null);
            return new TemplateDraft(templateKey, draftId, branch, previous.baseCommitSha(), commit.getName(),
                    document);
        } catch (NotificationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw registryFailure("Unable to update template draft", exception);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public TemplateDraft getDraft(String templateKey, String draftId) {
        lock.lock();
        try {
            String branch = findDraftBranch(templateKey, draftId);
            checkout(branch);
            TemplateDocument document = readOnlyTemplateOnCurrentBranch(templateKey);
            Manifest manifest = readManifest(findTemplateDirectory(
                    templateKey, document.channel(), document.locale()));
            return new TemplateDraft(templateKey, draftId, branch, manifest.baseCommitSha(),
                    document.commitSha(), document);
        } catch (Exception exception) {
            if (exception instanceof NotificationException notificationException)
                throw notificationException;
            throw registryFailure("Unable to load template draft", exception);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public TemplateDocument getPublishedTemplate(String templateKey, Channel channel, String locale) {
        lock.lock();
        try {
            fetch();
            checkoutPublished();
            String currentHead = head(requireGit());
            TemplateDocument document;
            try {
                document = readDocument(templateKey, channel, locale, currentHead,
                        findVersionForCommit(templateKey, channel, locale, currentHead));
            } catch (NotificationException exception) {
                if (!"TEMPLATE_NOT_FOUND".equals(exception.getErrorCode()))
                    throw exception;
                document = readLegacyDocument(templateKey, channel, locale, currentHead);
            }
            if (document.status() != TemplateStatus.PUBLISHED) {
                throw new NotificationException("TEMPLATE_NOT_PUBLISHED",
                        "Only published templates may be used for delivery", HttpStatus.NOT_FOUND);
            }
            return document;
        } catch (NotificationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw registryFailure("Unable to load published template", exception);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public TemplateDocument getTemplateVersion(
            String templateKey, Channel channel, String locale, String version) {
        lock.lock();
        try {
            Git active = requireGit();
            String tag = tagPrefix(templateKey, channel, locale) + requireVersion(version);
            Ref ref = active.getRepository().findRef("refs/tags/" + tag);
            if (ref == null) {
                throw new NotificationException("TEMPLATE_VERSION_NOT_FOUND",
                        "Template version was not found", HttpStatus.NOT_FOUND);
            }
            active.checkout().setName(ref.getName()).call();
            String commitSha = head(active);
            TemplateDocument document = readDocument(templateKey, channel, locale, commitSha, version);
            checkoutPublished();
            return document;
        } catch (NotificationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw registryFailure("Unable to load template version", exception);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<TemplateSummary> findTemplates(TemplateSearchCriteria criteria) {
        lock.lock();
        try {
            fetch();
            checkoutPublished();
            String currentHead = head(requireGit());
            List<TemplateSummary> summaries = new ArrayList<>();
            Set<String> nativeTemplates = new HashSet<>();
            for (Path manifestPath : manifestPaths()) {
                Manifest manifest = readManifest(manifestPath.getParent());
                TemplateDocument document = readDocument(manifest.templateKey(), manifest.channel(),
                        manifest.locale(), currentHead,
                        findVersionForCommit(manifest.templateKey(), manifest.channel(), manifest.locale(),
                                currentHead));
                nativeTemplates.add(templateIdentity(
                        document.templateKey(), document.channel(), document.locale()));
                if (matches(document, criteria)) {
                    summaries.add(summary(document));
                }
            }
            for (Path legacyPath : legacyTemplatePaths()) {
                TemplateDocument document = readLegacyEmailDocument(
                        legacyTemplateKey(legacyPath), legacyLocale(legacyPath), currentHead, legacyPath);
                if (!nativeTemplates.contains(templateIdentity(
                        document.templateKey(), document.channel(), document.locale()))
                        && matches(document, criteria)) {
                    summaries.add(summary(document));
                }
            }
            summaries.sort(Comparator.comparing(TemplateSummary::templateKey)
                    .thenComparing(summary -> summary.channel().name())
                    .thenComparing(TemplateSummary::locale));
            return List.copyOf(summaries);
        } catch (Exception exception) {
            throw registryFailure("Unable to list templates", exception);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<TemplateVersionSummary> findVersions(String templateKey, Channel channel, String locale) {
        lock.lock();
        try {
            String prefix = "refs/tags/" + tagPrefix(templateKey, channel, locale);
            List<TemplateVersionSummary> versions = new ArrayList<>();
            for (Ref tag : requireGit().tagList().call()) {
                if (!tag.getName().startsWith(prefix))
                    continue;
                RevCommit commit = parseCommit(tag);
                String version = tag.getName().substring(prefix.length());
                versions.add(new TemplateVersionSummary(version,
                        tag.getName().substring("refs/tags/".length()), commit.getName(),
                        commit.getAuthorIdent().getName(),
                        commit.getAuthorIdent().getWhenAsInstant(),
                        commit.getShortMessage()));
            }
            versions.sort(Comparator.comparing(TemplateVersionSummary::version).reversed());
            return List.copyOf(versions);
        } catch (Exception exception) {
            throw registryFailure("Unable to list template versions", exception);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public TemplateValidationResult validateDraft(String templateKey, String draftId) {
        lock.lock();
        try {
            checkout(findDraftBranch(templateKey, draftId));
            TemplateDocument document = readOnlyTemplateOnCurrentBranch(templateKey);
            return renderer.validate(document, document.sampleData());
        } catch (Exception exception) {
            if (exception instanceof NotificationException notificationException)
                throw notificationException;
            throw registryFailure("Unable to validate template draft", exception);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public TemplatePreviewResult previewDraft(
            String templateKey, String draftId, Map<String, Object> sampleData) {
        lock.lock();
        try {
            checkout(findDraftBranch(templateKey, draftId));
            TemplateDocument document = readOnlyTemplateOnCurrentBranch(templateKey);
            Map<String, Object> data = sampleData == null || sampleData.isEmpty()
                    ? document.sampleData()
                    : sampleData;
            TemplateValidationResult validation = renderer.validate(document, data);
            RenderedTemplate rendered = validation.valid() ? renderer.render(document, data) : null;
            return new TemplatePreviewResult(validation, rendered, document.commitSha());
        } catch (Exception exception) {
            if (exception instanceof NotificationException notificationException)
                throw notificationException;
            throw registryFailure("Unable to preview template draft", exception);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public TemplatePublicationResult publishDraft(
            String templateKey,
            String draftId,
            String expectedCommitSha,
            String actorPublicId) {
        lock.lock();
        try {
            Git active = requireGit();
            String branch = findDraftBranch(templateKey, draftId);
            checkout(branch);
            assertExpectedHead(expectedCommitSha);
            TemplateDocument draft = readOnlyTemplateOnCurrentBranch(templateKey);
            TemplateValidationResult validation = renderer.validate(draft, draft.sampleData());
            if (!validation.valid()) {
                throw new NotificationException("TEMPLATE_VALIDATION_FAILED",
                        String.join("; ", validation.errors()), HttpStatus.UNPROCESSABLE_ENTITY);
            }
            Manifest manifest = readManifest(findTemplateDirectory(
                    templateKey, draft.channel(), draft.locale()));
            checkoutPublished();
            if (!head(active).equals(manifest.baseCommitSha())) {
                throw conflict("Published template changed after this draft was created");
            }
            checkout(branch);
            writeDocument(draft, TemplateStatus.PUBLISHED, manifest.baseCommitSha());
            commit("Publish " + templateKey);
            Ref branchRef = active.getRepository().findRef("refs/heads/" + branch);
            checkoutPublished();
            MergeResult result = active.merge()
                    .include(branchRef)
                    .setFastForward(MergeCommand.FastForwardMode.NO_FF)
                    .setMessage("Publish " + templateKey)
                    .call();
            if (!result.getMergeStatus().isSuccessful()) {
                throw conflict("Template publication has a Git merge conflict");
            }
            String commitSha = head(active);
            String version = nextVersion(templateKey, draft.channel(), draft.locale());
            String tag = tagPrefix(templateKey, draft.channel(), draft.locale()) + version;
            active.tag().setName(tag).setMessage("Published " + templateKey + " " + version)
                    .setTagger(person()).call();
            push();
            invalidate(templateKey, draft.channel(), draft.locale());
            audit(actorPublicId, "PUBLISH", templateKey, commitSha, version);
            deleteRemoteBranch(branch);
            deleteBranch(branch);
            return new TemplatePublicationResult(templateKey, draft.channel(), draft.locale(),
                    commitSha, version, tag, false);
        } catch (NotificationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw registryFailure("Unable to publish template draft", exception);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public TemplatePublicationResult rollback(
            String templateKey,
            Channel channel,
            String locale,
            String targetVersion,
            String actorPublicId) {
        lock.lock();
        try {
            TemplateDocument target = getTemplateVersion(templateKey, channel, locale, targetVersion);
            TemplateValidationResult validation = renderer.validate(target, target.sampleData());
            if (!validation.valid()) {
                throw new NotificationException("ROLLBACK_TEMPLATE_INVALID",
                        String.join("; ", validation.errors()), HttpStatus.UNPROCESSABLE_ENTITY);
            }
            checkoutPublished();
            writeDocument(target, TemplateStatus.PUBLISHED, head(requireGit()));
            RevCommit commit = commit("Rollback " + templateKey + " to " + targetVersion);
            String version = nextVersion(templateKey, channel, locale);
            String tag = tagPrefix(templateKey, channel, locale) + version;
            requireGit().tag().setName(tag)
                    .setMessage("Rollback " + templateKey + " to " + targetVersion)
                    .setTagger(person()).call();
            push();
            invalidate(templateKey, channel, locale);
            audit(actorPublicId, "ROLLBACK", templateKey, commit.getName(), version);
            return new TemplatePublicationResult(templateKey, channel, locale,
                    commit.getName(), version, tag, true);
        } catch (NotificationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw registryFailure("Unable to roll back template", exception);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void deleteDraft(String templateKey, String draftId, String actorPublicId) {
        lock.lock();
        try {
            String branch = findDraftBranch(templateKey, draftId);
            checkoutPublished();
            deleteRemoteBranch(branch);
            deleteBranch(branch);
            audit(actorPublicId, "DELETE_DRAFT", templateKey, null, null);
        } catch (Exception exception) {
            throw registryFailure("Unable to delete template draft", exception);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void archive(String templateKey, Channel channel, String locale, String actorPublicId) {
        changePublishedStatus(templateKey, channel, locale, actorPublicId, TemplateStatus.ARCHIVED);
    }

    @Override
    public void restore(String templateKey, Channel channel, String locale, String actorPublicId) {
        changePublishedStatus(templateKey, channel, locale, actorPublicId, TemplateStatus.PUBLISHED);
    }

    @Override
    public RegistryHealth health() {
        lock.lock();
        try {
            if (git == null) {
                return new RegistryHealth(false, "JGit", publishedBranch, null, initializationError);
            }
            String message = lastRefreshError == null
                    ? "Template registry is available"
                    : "Template registry is available; last automatic refresh failed: " + lastRefreshError;
            return new RegistryHealth(true, "JGit", publishedBranch, head(git), message);
        } catch (Exception exception) {
            return new RegistryHealth(false, "JGit", publishedBranch, null, safeMessage(exception));
        } finally {
            lock.unlock();
        }
    }

    private void changePublishedStatus(
            String templateKey,
            Channel channel,
            String locale,
            String actorPublicId,
            TemplateStatus status) {
        lock.lock();
        try {
            checkoutPublished();
            TemplateDocument document = readDocument(templateKey, channel, locale,
                    head(requireGit()), findVersionForCommit(templateKey, channel, locale, head(requireGit())));
            writeDocument(document, status, head(requireGit()));
            RevCommit commit = commit((status == TemplateStatus.ARCHIVED ? "Archive " : "Restore ") + templateKey);
            push();
            invalidate(templateKey, channel, locale);
            audit(actorPublicId, status.name(), templateKey, commit.getName(), null);
        } catch (Exception exception) {
            if (exception instanceof NotificationException notificationException)
                throw notificationException;
            throw registryFailure("Unable to change template status", exception);
        } finally {
            lock.unlock();
        }
    }

    private void writeContent(
            String templateKey, TemplateContent content, TemplateStatus status, String baseCommitSha)
            throws IOException {
        TemplateDocument document = new TemplateDocument(templateKey, content.displayName(), content.description(),
                content.category(), content.channel(), content.locale(), status, content.variablesSchema(),
                content.sampleData(), content.subject(), content.htmlContent(), content.textContent(),
                null, null, null);
        writeDocument(document, status, baseCommitSha);
    }

    private void writeDocument(
            TemplateDocument document, TemplateStatus status, String baseCommitSha) throws IOException {
        Path directory = templateDirectory(document.category(), document.templateKey(),
                document.channel(), document.locale());
        Files.createDirectories(directory);
        Manifest manifest = new Manifest(document.templateKey(), document.displayName(), document.description(),
                document.category(), document.channel(), document.locale(), status,
                document.variablesSchema(), document.sampleData(), baseCommitSha);
        write(directory.resolve(MANIFEST), objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(manifest));
        write(directory.resolve(SUBJECT), document.subject());
        write(directory.resolve(HTML), document.htmlContent());
        write(directory.resolve(TEXT), document.textContent());
    }

    private TemplateDocument readOnlyTemplateOnCurrentBranch(String templateKey) throws Exception {
        List<Path> matches = manifestPaths().stream().filter(path -> {
            try {
                return templateKey.equals(readManifest(path.getParent()).templateKey());
            } catch (Exception exception) {
                return false;
            }
        }).toList();
        if (matches.size() != 1) {
            throw new NotificationException("TEMPLATE_DRAFT_NOT_FOUND",
                    "The draft does not contain exactly one matching template", HttpStatus.NOT_FOUND);
        }
        Manifest manifest = readManifest(matches.getFirst().getParent());
        return readDocument(templateKey, manifest.channel(), manifest.locale(), head(requireGit()), null);
    }

    private TemplateDocument readDocument(
            String templateKey, Channel channel, String locale, String commitSha, String version) throws Exception {
        Path directory = findTemplateDirectory(templateKey, channel, locale);
        Manifest manifest = readManifest(directory);
        RevCommit commit = parseCommit(ObjectId.fromString(commitSha));
        return new TemplateDocument(manifest.templateKey(), manifest.displayName(), manifest.description(),
                manifest.category(), manifest.channel(), manifest.locale(), manifest.status(),
                manifest.variablesSchema(), manifest.sampleData(),
                read(directory.resolve(SUBJECT)), read(directory.resolve(HTML)), read(directory.resolve(TEXT)),
                commitSha, version, commit.getCommitterIdent().getWhenAsInstant());
    }

    private TemplateDocument readLegacyDocument(
            String templateKey, Channel channel, String locale, String commitSha) throws Exception {
        Path path = findLegacyEmailPath(templateKey, locale);
        if (channel == Channel.EMAIL) {
            return readLegacyEmailDocument(templateKey, locale, commitSha, path);
        }
        if (channel == Channel.IN_APP || channel == Channel.WEB_PUSH) {
            return readLegacyCompactDocument(templateKey, channel, locale, commitSha, path);
        }
        throw new NotificationException("TEMPLATE_NOT_FOUND",
                "Template " + templateKey + "/" + channel + "/" + locale + " was not found",
                HttpStatus.NOT_FOUND);
    }

    private TemplateDocument readLegacyEmailDocument(
            String templateKey, String locale, String commitSha, Path path) throws Exception {
        String html = read(path);
        String subject = legacySubject(html, displayName(templateKey));
        RevCommit commit = parseCommit(ObjectId.fromString(commitSha));
        return new TemplateDocument(
                templateKey,
                displayName(templateKey),
                "Git email template: " + workingDirectory.relativize(path).toString().replace('\\', '/'),
                legacyCategory(path),
                Channel.EMAIL,
                locale,
                TemplateStatus.PUBLISHED,
                legacyVariables(html),
                Map.of(),
                subject,
                html,
                legacyPlainText(html),
                commitSha,
                null,
                commit.getCommitterIdent().getWhenAsInstant());
    }

    private TemplateDocument readLegacyCompactDocument(
            String templateKey,
            Channel channel,
            String locale,
            String commitSha,
            Path path) throws Exception {
        String html = read(path);
        String subject = legacySubject(html, displayName(templateKey));
        String text = legacySummaryText(html, subject);
        RevCommit commit = parseCommit(ObjectId.fromString(commitSha));
        return new TemplateDocument(
                templateKey,
                displayName(templateKey),
                "Compact channel content derived from Git email template: "
                        + workingDirectory.relativize(path).toString().replace('\\', '/'),
                legacyCategory(path),
                channel,
                locale,
                TemplateStatus.PUBLISHED,
                legacyVariables(html),
                Map.of(),
                subject,
                "<p>" + text + "</p>",
                text,
                commitSha,
                null,
                commit.getCommitterIdent().getWhenAsInstant());
    }

    private Path findLegacyEmailPath(String templateKey, String locale) throws IOException {
        if (locale == null || !locale.matches("[a-z]{2}-[A-Z]{2}")) {
            throw new NotificationException("INVALID_TEMPLATE_LOCALE",
                    "Locale must use language-REGION format", HttpStatus.BAD_REQUEST);
        }
        List<String> languages = new ArrayList<>();
        languages.add(locale.substring(0, 2).toLowerCase(Locale.ROOT));
        for (String fallback : List.of("vi", "en")) {
            if (!languages.contains(fallback))
                languages.add(fallback);
        }
        for (String language : languages) {
            Path localeDirectory = safePath(workingDirectory.resolve("email").resolve(language));
            Path match = findLegacyEmailPath(templateKey, localeDirectory);
            if (match != null)
                return match;
        }
        throw new NotificationException("TEMPLATE_NOT_FOUND",
                "Git email template " + templateKey + "/" + locale + " was not found",
                HttpStatus.NOT_FOUND);
    }

    private Path findLegacyEmailPath(String templateKey, Path localeDirectory) throws IOException {
        if (!Files.isDirectory(localeDirectory, LinkOption.NOFOLLOW_LINKS))
            return null;
        String alias = LEGACY_TEMPLATE_ALIASES.get(templateKey);
        if (alias != null) {
            Path aliased = safePath(localeDirectory.resolve(alias));
            if (Files.isRegularFile(aliased, LinkOption.NOFOLLOW_LINKS))
                return aliased;
        }
        String fileName = templateKey.toLowerCase(Locale.ROOT) + ".html";
        try (Stream<Path> paths = Files.walk(localeDirectory)) {
            return paths.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> fileName.equals(path.getFileName().toString()))
                    .findFirst()
                    .orElse(null);
        }
    }

    private List<Path> legacyTemplatePaths() throws IOException {
        Path emailDirectory = safePath(workingDirectory.resolve("email"));
        if (!Files.isDirectory(emailDirectory, LinkOption.NOFOLLOW_LINKS))
            return List.of();

        List<Path> templates = new ArrayList<>();
        for (String language : List.of("vi", "en")) {
            Path localeDirectory = safePath(emailDirectory.resolve(language));
            if (!Files.isDirectory(localeDirectory, LinkOption.NOFOLLOW_LINKS))
                continue;
            try (Stream<Path> paths = Files.walk(localeDirectory)) {
                templates.addAll(paths
                        .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                        .filter(path -> path.getFileName().toString().endsWith(".html"))
                        .toList());
            }
        }
        return List.copyOf(templates);
    }

    private String legacyTemplateKey(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.substring(0, fileName.length() - ".html".length())
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_");
    }

    private String legacyLocale(Path path) {
        Path relative = workingDirectory.relativize(path);
        if (relative.getNameCount() < 3 || !"email".equals(relative.getName(0).toString())) {
            throw new NotificationException("GIT_PATH_REJECTED",
                    "Legacy template path is invalid", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return switch (relative.getName(1).toString().toLowerCase(Locale.ROOT)) {
            case "vi" -> "vi-VN";
            case "en" -> "en-US";
            default -> throw new NotificationException("INVALID_TEMPLATE_LOCALE",
                    "Unsupported legacy template locale", HttpStatus.UNPROCESSABLE_ENTITY);
        };
    }

    private Category legacyCategory(Path path) {
        Path relative = workingDirectory.relativize(path);
        String domain = relative.getNameCount() >= 4
                ? relative.getName(relative.getNameCount() - 2).toString().toLowerCase(Locale.ROOT)
                : "";
        return switch (domain) {
            case "auth" -> Category.SECURITY;
            case "promotion", "membership" -> Category.MARKETING;
            case "system" -> Category.OPERATIONAL;
            default -> Category.TRANSACTIONAL;
        };
    }

    private Map<String, VariableDefinition> legacyVariables(String html) {
        Map<String, VariableDefinition> variables = new LinkedHashMap<>();
        Matcher matcher = LEGACY_EXPRESSION.matcher(html);
        while (matcher.find()) {
            variables.putIfAbsent(matcher.group(1), new VariableDefinition("string", false));
        }
        return Map.copyOf(variables);
    }

    private String legacySubject(String html, String fallback) {
        Matcher matcher = HTML_TITLE.matcher(html);
        if (!matcher.find())
            return fallback;
        String subject = HtmlUtils.htmlUnescape(matcher.group(1).replaceAll("\\s+", " ").trim());
        return subject.isBlank() ? fallback : subject;
    }

    private String legacySummaryText(String html, String fallback) {
        List<String> parts = new ArrayList<>();
        Matcher heading = FIRST_HEADING.matcher(html);
        if (heading.find()) {
            String text = legacyTextFragment(heading.group(1));
            if (!text.isBlank())
                parts.add(text);
        }
        Matcher paragraph = FIRST_PARAGRAPH.matcher(html);
        if (paragraph.find()) {
            String text = legacyTextFragment(paragraph.group(1));
            if (!text.isBlank() && !parts.contains(text))
                parts.add(text);
        }
        return parts.isEmpty() ? fallback : String.join("\n\n", parts);
    }

    private String legacyTextFragment(String html) {
        return HtmlUtils.htmlUnescape(html.replaceAll("(?s)<[^>]+>", " "))
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String legacyPlainText(String html) {
        String text = html
                .replaceAll("(?is)<style[^>]*>.*?</style>", " ")
                .replaceAll("(?is)<script[^>]*>.*?</script>", " ")
                .replaceAll("(?is)<!--.*?-->", " ")
                .replaceAll("(?i)<br\\s*/?>|</p>|</div>|</tr>|</h[1-6]>", "\n")
                .replaceAll("(?s)<[^>]+>", " ");
        return HtmlUtils.htmlUnescape(text)
                .replace("\r", "")
                .replaceAll("[\\t ]+", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String displayName(String templateKey) {
        String lower = templateKey.toLowerCase(Locale.ROOT).replace('_', ' ');
        if (lower.isBlank())
            return templateKey;
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private String templateIdentity(String templateKey, Channel channel, String locale) {
        return templateKey + "|" + channel + "|" + locale;
    }

    private TemplateSummary summary(TemplateDocument document) {
        return new TemplateSummary(document.templateKey(), document.displayName(),
                document.category(), document.channel(), document.locale(), document.status(),
                document.version(), document.commitSha(), document.committedAt());
    }

    private Path findTemplateDirectory(String templateKey, Channel channel, String locale) throws IOException {
        for (Path path : manifestPaths()) {
            Manifest manifest = readManifest(path.getParent());
            if (templateKey.equals(manifest.templateKey())
                    && channel == manifest.channel() && locale.equals(manifest.locale())) {
                return path.getParent();
            }
        }
        throw new NotificationException("TEMPLATE_NOT_FOUND",
                "Template " + templateKey + "/" + channel + "/" + locale + " was not found",
                HttpStatus.NOT_FOUND);
    }

    private List<Path> manifestPaths() throws IOException {
        Path templates = safePath(workingDirectory.resolve("templates"));
        if (!Files.exists(templates))
            return List.of();
        try (Stream<Path> paths = Files.walk(templates)) {
            return paths.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> MANIFEST.equals(path.getFileName().toString()))
                    .toList();
        }
    }

    private Manifest readManifest(Path directory) throws IOException {
        Path manifest = safePath(directory.resolve(MANIFEST));
        if (Files.isSymbolicLink(manifest)) {
            throw new NotificationException("GIT_PATH_REJECTED", "Symbolic links are not allowed",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return objectMapper.readValue(read(manifest), Manifest.class);
    }

    private Path templateDirectory(Category category, String templateKey, Channel channel, String locale) {
        String key = safeSegment(templateKey.toLowerCase(Locale.ROOT).replace('_', '-'));
        return safePath(workingDirectory.resolve("templates")
                .resolve(category.name().toLowerCase(Locale.ROOT))
                .resolve(key)
                .resolve(channel.name().toLowerCase(Locale.ROOT).replace('_', '-'))
                .resolve(safeSegment(locale)));
    }

    private void write(Path path, String value) throws IOException {
        Path safe = safePath(path);
        byte[] content = (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
        if (content.length > maxTemplateBytes) {
            throw new NotificationException("TEMPLATE_TOO_LARGE",
                    "A template file exceeds the configured size limit", HttpStatus.PAYLOAD_TOO_LARGE);
        }
        Files.write(safe, content);
    }

    private String read(Path path) throws IOException {
        Path safe = safePath(path);
        if (!Files.exists(safe) || Files.isSymbolicLink(safe)) {
            throw new NotificationException("TEMPLATE_FILE_NOT_FOUND",
                    "Required template file is missing", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (Files.size(safe) > maxTemplateBytes) {
            throw new NotificationException("TEMPLATE_TOO_LARGE",
                    "A template file exceeds the configured size limit", HttpStatus.PAYLOAD_TOO_LARGE);
        }
        return Files.readString(safe, StandardCharsets.UTF_8);
    }

    private Path safePath(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        if (!normalized.startsWith(workingDirectory)) {
            throw new NotificationException("GIT_PATH_REJECTED",
                    "Template path escapes the registry working directory", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String findDraftBranch(String templateKey, String draftId) throws Exception {
        requireGitName(draftId, "draft id");
        String prefix = "refs/heads/draft/" + templateKey.toLowerCase(Locale.ROOT) + "/";
        return requireGit().branchList().call().stream()
                .map(Ref::getName)
                .filter(name -> name.startsWith(prefix) && name.endsWith("/" + draftId))
                .map(name -> name.substring("refs/heads/".length()))
                .findFirst()
                .orElseThrow(() -> new NotificationException("TEMPLATE_DRAFT_NOT_FOUND",
                        "Template draft was not found", HttpStatus.NOT_FOUND));
    }

    private void assertExpectedHead(String expectedCommitSha) throws Exception {
        if (expectedCommitSha == null || !expectedCommitSha.matches("[a-fA-F0-9]{40}")) {
            throw new NotificationException("EXPECTED_COMMIT_REQUIRED",
                    "A valid expectedCommitSha is required", HttpStatus.BAD_REQUEST);
        }
        if (!head(requireGit()).equalsIgnoreCase(expectedCommitSha)) {
            throw conflict("The draft was modified by another administrator");
        }
    }

    private void checkoutPublished() throws Exception {
        checkout(publishedBranch);
    }

    private void checkout(String branch) throws Exception {
        requireGit().checkout().setForced(true).setName(requireGitName(branch, "branch")).call();
    }

    private RevCommit commit(String message) throws Exception {
        Git active = requireGit();
        active.add().addFilepattern(".").call();
        active.add().setUpdate(true).addFilepattern(".").call();
        return active.commit().setMessage(message).setAuthor(person()).setCommitter(person()).call();
    }

    private void deleteBranch(String branch) throws Exception {
        requireGit().branchDelete().setBranchNames(branch).setForce(true).call();
    }

    private String head(Git active) throws IOException {
        ObjectId head = active.getRepository().resolve("HEAD");
        if (head == null) {
            throw new NotificationException("TEMPLATE_REGISTRY_EMPTY",
                    "Template repository has no commits", HttpStatus.SERVICE_UNAVAILABLE);
        }
        return head.getName();
    }

    private RevCommit parseCommit(Ref ref) throws IOException {
        Ref peeled = requireGit().getRepository().getRefDatabase().peel(ref);
        return parseCommit(peeled.getPeeledObjectId() == null
                ? ref.getObjectId()
                : peeled.getPeeledObjectId());
    }

    private RevCommit parseCommit(ObjectId objectId) throws IOException {
        try (org.eclipse.jgit.revwalk.RevWalk walk = new org.eclipse.jgit.revwalk.RevWalk(
                requireGit().getRepository())) {
            return walk.parseCommit(objectId);
        }
    }

    private String nextVersion(String templateKey, Channel channel, String locale) throws Exception {
        int maximum = 0;
        String prefix = "refs/tags/" + tagPrefix(templateKey, channel, locale) + "v";
        for (Ref ref : requireGit().tagList().call()) {
            if (ref.getName().startsWith(prefix)) {
                String suffix = ref.getName().substring(prefix.length());
                if (suffix.matches("\\d{6}"))
                    maximum = Math.max(maximum, Integer.parseInt(suffix));
            }
        }
        return "v%06d".formatted(maximum + 1);
    }

    private String findVersionForCommit(
            String templateKey, Channel channel, String locale, String commitSha) throws Exception {
        String prefix = "refs/tags/" + tagPrefix(templateKey, channel, locale);
        for (Ref ref : requireGit().tagList().call()) {
            if (ref.getName().startsWith(prefix) && parseCommit(ref).getName().equals(commitSha)) {
                return ref.getName().substring(prefix.length());
            }
        }
        return null;
    }

    private String tagPrefix(String templateKey, Channel channel, String locale) {
        return "notification-template/" + templateKey + "/" + channel.name() + "/" + locale + "/";
    }

    private String requireVersion(String version) {
        if (version == null || !version.matches("v\\d{6}")) {
            throw new NotificationException("INVALID_TEMPLATE_VERSION",
                    "Version must use v000001 format", HttpStatus.BAD_REQUEST);
        }
        return version;
    }

    private boolean matches(TemplateDocument document, TemplateSearchCriteria criteria) {
        if (criteria == null)
            return true;
        String query = criteria.query() == null ? "" : criteria.query().trim().toLowerCase(Locale.ROOT);
        boolean queryMatch = query.isEmpty()
                || document.templateKey().toLowerCase(Locale.ROOT).contains(query)
                || document.displayName().toLowerCase(Locale.ROOT).contains(query);
        boolean archived = document.status() == TemplateStatus.ARCHIVED;
        return queryMatch
                && (criteria.category() == null || criteria.category() == document.category())
                && (criteria.channel() == null || criteria.channel() == document.channel())
                && (criteria.locale() == null || criteria.locale().equals(document.locale()))
                && (criteria.archived() == null || criteria.archived() == archived);
    }

    private void invalidate(String key, Channel channel, String locale) {
        try {
            StringRedisTemplate redis = redisProvider.getIfAvailable();
            if (redis != null) {
                redis.delete(List.of(
                        "notification:template:" + key + ":" + channel + ":" + locale + ":published",
                        "notification:template:" + key + ":" + channel + ":" + locale + ":compiled"));
            }
        } catch (RuntimeException ignored) {
            // Git is authoritative. Cache failure must not block publishing.
        }
    }

    private void audit(
            String actorPublicId, String action, String templateKey, String commitSha, String version) {
        NotificationAuditLog audit = new NotificationAuditLog();
        audit.setActorPublicId(actorPublicId == null ? "system" : actorPublicId);
        audit.setAction(action);
        audit.setTargetType("NOTIFICATION_TEMPLATE");
        audit.setTargetPublicId(templateKey);
        try {
            audit.setMetadataJson(objectMapper.writeValueAsString(
                    Map.of("commitSha", commitSha == null ? "" : commitSha,
                            "version", version == null ? "" : version)));
        } catch (Exception exception) {
            audit.setMetadataJson("{}");
        }
        auditRepository.save(audit);
    }

    private void fetch() throws Exception {
        if (git != null && remoteUri != null && !remoteUri.isBlank()) {
            try {
                git.fetch().setCredentialsProvider(credentials()).setTimeout(fetchTimeoutSeconds).call();
            } catch (org.eclipse.jgit.api.errors.TransportException ex) {
                log.warn("Git fetch with configured credentials failed ({}), falling back to unauthenticated fetch...",
                        ex.getMessage());
                git.fetch().setCredentialsProvider(null).setTimeout(fetchTimeoutSeconds).call();
            }
        }
    }

    private String fastForwardPublishedBranch() throws Exception {
        Git active = requireGit();
        if (!active.status().call().isClean()) {
            throw new IllegalStateException(
                    "Template registry has local changes; automatic refresh was skipped");
        }

        fetch();
        ObjectId remoteHead = active.getRepository().resolve(
                Constants.R_REMOTES + "origin/" + publishedBranch);
        ObjectId localHead = active.getRepository().resolve(
                Constants.R_HEADS + publishedBranch);
        if (remoteHead == null) {
            throw new IllegalStateException(
                    "Remote branch origin/" + publishedBranch + " was not found");
        }
        if (localHead == null) {
            throw new IllegalStateException(
                    "Local published branch " + publishedBranch + " was not found");
        }
        if (localHead.equals(remoteHead)) {
            return null;
        }

        try (RevWalk walk = new RevWalk(active.getRepository())) {
            RevCommit localCommit = walk.parseCommit(localHead);
            RevCommit remoteCommit = walk.parseCommit(remoteHead);
            if (!walk.isMergedInto(localCommit, remoteCommit)) {
                if (walk.isMergedInto(remoteCommit, localCommit)) {
                    log.debug("Local template branch {} is ahead of origin/{}; refresh skipped",
                            publishedBranch, publishedBranch);
                    return null;
                }
                throw new IllegalStateException(
                        "Local and remote template branches have diverged; automatic refresh was skipped");
            }
        }

        checkoutPublished();
        MergeResult result = active.merge()
                .include(remoteHead)
                .setFastForward(MergeCommand.FastForwardMode.FF_ONLY)
                .call();
        if (!result.getMergeStatus().isSuccessful()
                || !remoteHead.equals(active.getRepository().resolve(
                        Constants.R_HEADS + publishedBranch))) {
            throw new IllegalStateException(
                    "Unable to fast-forward template branch: " + result.getMergeStatus());
        }
        return remoteHead.getName();
    }

    private void push() throws Exception {
        if (remoteUri == null || remoteUri.isBlank())
            return;
        pushRef(Constants.R_HEADS + publishedBranch, Constants.R_HEADS + publishedBranch,
                "published branch " + publishedBranch);
        try {
            assertPushSucceeded(
                    requireGit().push()
                            .setRemote("origin")
                            .setPushTags()
                            .setCredentialsProvider(credentials())
                            .setTimeout(fetchTimeoutSeconds)
                            .call(),
                    "template version tags",
                    true);
        } catch (Exception ex) {
            log.warn("Failed to push template tags to remote Git origin: {}", ex.getMessage());
        }
    }

    private void pushBranch(String branch) throws Exception {
        String safeBranch = requireGitName(branch, "branch");
        pushRef(Constants.R_HEADS + safeBranch, Constants.R_HEADS + safeBranch,
                "draft branch " + safeBranch);
    }

    private void deleteRemoteBranch(String branch) throws Exception {
        if (remoteUri == null || remoteUri.isBlank())
            return;
        String safeBranch = requireGitName(branch, "branch");
        String remoteRef = Constants.R_HEADS + safeBranch;
        try {
            assertPushSucceeded(
                    requireGit().push()
                            .setRemote("origin")
                            .setRefSpecs(new RefSpec(":" + remoteRef))
                            .setCredentialsProvider(credentials())
                            .setTimeout(fetchTimeoutSeconds)
                            .call(),
                    "delete remote draft branch " + safeBranch);
        } catch (Exception ex) {
            log.warn("Failed to delete remote draft branch {} on Git origin: {}", safeBranch, ex.getMessage());
        }
    }

    private void pushRef(String localRef, String remoteRef, String description) throws Exception {
        if (remoteUri == null || remoteUri.isBlank())
            return;
        try {
            assertPushSucceeded(
                    requireGit().push()
                            .setRemote("origin")
                            .setRefSpecs(new RefSpec(localRef + ":" + remoteRef))
                            .setCredentialsProvider(credentials())
                            .setTimeout(fetchTimeoutSeconds)
                            .call(),
                    description);
        } catch (Exception ex) {
            log.warn("Failed to push {} to remote Git origin: {}", description, ex.getMessage());
        }
    }

    private void assertPushSucceeded(Iterable<PushResult> results, String description) throws IOException {
        assertPushSucceeded(results, description, false);
    }

    private void assertPushSucceeded(
            Iterable<PushResult> results, String description, boolean allowNoUpdates) throws IOException {
        boolean updateReported = false;
        for (PushResult result : results) {
            for (RemoteRefUpdate update : result.getRemoteUpdates()) {
                updateReported = true;
                RemoteRefUpdate.Status status = update.getStatus();
                if (status != RemoteRefUpdate.Status.OK
                        && status != RemoteRefUpdate.Status.UP_TO_DATE) {
                    String message = update.getMessage();
                    throw new IOException("Git push failed for " + description + " ("
                            + update.getRemoteName() + "): " + status
                            + (message == null || message.isBlank() ? "" : " - " + message));
                }
            }
        }
        if (!updateReported && !allowNoUpdates) {
            throw new IOException("Git push returned no result for " + description);
        }
    }

    private CredentialsProvider credentials() {
        return new UsernamePasswordCredentialsProvider(username == null ? "" : username,
                accessToken == null ? "" : accessToken);
    }

    private PersonIdent person() {
        return new PersonIdent(authorName, authorEmail);
    }

    private Git requireGit() {
        if (git == null) {
            throw new NotificationException("TEMPLATE_REGISTRY_UNAVAILABLE",
                    initializationError == null ? "Template registry is unavailable" : initializationError,
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        return git;
    }

    private void requireCommand(String templateKey, String actor, TemplateContent content) {
        if (templateKey == null || !templateKey.matches("[A-Z0-9_]{3,100}")) {
            throw new NotificationException("INVALID_TEMPLATE_KEY",
                    "Template key must contain only uppercase letters, digits, and underscores",
                    HttpStatus.BAD_REQUEST);
        }
        if (actor == null || actor.isBlank() || content == null || content.category() == null
                || content.channel() == null || content.locale() == null) {
            throw new NotificationException("INVALID_TEMPLATE_DRAFT",
                    "Actor, category, channel, locale, and content are required", HttpStatus.BAD_REQUEST);
        }
    }

    private String safeSegment(String value) {
        String safe = value == null ? ""
                : value.toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9._-]", "-").replaceAll("-+", "-");
        if (safe.isBlank() || safe.equals(".") || safe.equals("..")) {
            throw new NotificationException("GIT_PATH_REJECTED", "Invalid Git path segment", HttpStatus.BAD_REQUEST);
        }
        return safe;
    }

    private static String requireGitName(String value, String field) {
        if (value == null || !value.matches("[A-Za-z0-9._/-]{1,240}")
                || value.contains("..") || value.startsWith("/") || value.endsWith("/")) {
            throw new NotificationException("INVALID_GIT_REFERENCE",
                    "Invalid " + field, HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    private NotificationException conflict(String message) {
        return new NotificationException("TEMPLATE_CONFLICT", message, HttpStatus.CONFLICT);
    }

    private NotificationException registryFailure(String message, Exception exception) {
        return new NotificationException("TEMPLATE_REGISTRY_FAILURE",
                message + ": " + safeMessage(exception), HttpStatus.SERVICE_UNAVAILABLE);
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private void closeGit() {
        if (git != null)
            git.close();
        git = null;
    }

    private record Manifest(
            String templateKey,
            String displayName,
            String description,
            Category category,
            Channel channel,
            String locale,
            TemplateStatus status,
            Map<String, VariableDefinition> variablesSchema,
            Map<String, Object> sampleData,
            String baseCommitSha) {

        private Manifest {
            variablesSchema = variablesSchema == null ? Map.of() : Map.copyOf(variablesSchema);
            sampleData = sampleData == null ? Map.of() : Map.copyOf(sampleData);
        }
    }
}
