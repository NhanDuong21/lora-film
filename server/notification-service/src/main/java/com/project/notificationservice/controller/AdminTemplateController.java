package com.project.notificationservice.controller;

import com.project.notificationservice.api.ApiResponse;
import com.project.notificationservice.domain.NotificationTypes.Category;
import com.project.notificationservice.domain.NotificationTypes.Channel;
import com.project.notificationservice.service.NotificationApplicationService;
import com.project.notificationservice.service.NotificationCommands.AcceptedNotification;
import com.project.notificationservice.service.NotificationCommands.CreateNotificationCommand;
import com.project.notificationservice.template.TemplateRegistry;
import com.project.notificationservice.template.TemplateContractCoverageService;
import com.project.notificationservice.template.TemplateRegistry.CreateTemplateDraftCommand;
import com.project.notificationservice.template.TemplateRegistry.TemplateDraft;
import com.project.notificationservice.template.TemplateRegistry.TemplatePreviewResult;
import com.project.notificationservice.template.TemplateRegistry.TemplatePublicationResult;
import com.project.notificationservice.template.TemplateRegistry.TemplateSearchCriteria;
import com.project.notificationservice.template.TemplateRegistry.TemplateSummary;
import com.project.notificationservice.template.TemplateRegistry.TemplateValidationResult;
import com.project.notificationservice.template.TemplateRegistry.TemplateVersionSummary;
import com.project.notificationservice.template.TemplateRegistry.UpdateTemplateDraftCommand;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/notification-templates")
public class AdminTemplateController {

    private final TemplateRegistry registry;
    private final NotificationApplicationService notificationService;
    private final TemplateContractCoverageService coverageService;

    public AdminTemplateController(
            TemplateRegistry registry,
            NotificationApplicationService notificationService,
            TemplateContractCoverageService coverageService) {
        this.registry = registry;
        this.notificationService = notificationService;
        this.coverageService = coverageService;
    }

    @GetMapping
    public ApiResponse<List<TemplateSummary>> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) Channel channel,
            @RequestParam(required = false) String locale,
            @RequestParam(required = false) Boolean archived) {
        return ApiResponse.success(registry.findTemplates(
                new TemplateSearchCriteria(query, category, channel, locale, archived)));
    }

    @GetMapping("/coverage")
    public ApiResponse<TemplateContractCoverageService.CoverageReport> coverage() {
        return ApiResponse.success(coverageService.inspect());
    }

    @GetMapping("/{templateKey}")
    public ApiResponse<TemplateRegistry.TemplateDocument> published(
            @PathVariable String templateKey,
            @RequestParam Channel channel,
            @RequestParam String locale) {
        return ApiResponse.success(registry.getPublishedTemplate(templateKey, channel, locale));
    }

    @PostMapping("/{templateKey}/preview-published")
    public ApiResponse<TemplatePreviewResult> previewPublished(
            @PathVariable String templateKey,
            @RequestParam Channel channel,
            @RequestParam String locale,
            @RequestBody(required = false) Map<String, Object> sampleData) {
        return ApiResponse.success(registry.previewPublished(
                templateKey, channel, locale, sampleData));
    }

    @PostMapping("/drafts")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TemplateDraft> createDraft(
            Authentication authentication,
            @Valid @RequestBody DraftCreateRequest request) {
        return ApiResponse.success(registry.createDraft(new CreateTemplateDraftCommand(
                request.templateKey(), actor(authentication), request.content())));
    }

    @GetMapping("/{templateKey}/drafts/{draftId}")
    public ApiResponse<TemplateDraft> getDraft(
            @PathVariable String templateKey,
            @PathVariable String draftId) {
        return ApiResponse.success(registry.getDraft(templateKey, draftId));
    }

    @PutMapping("/{templateKey}/drafts/{draftId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TemplateDraft> updateDraft(
            @PathVariable String templateKey,
            @PathVariable String draftId,
            @RequestHeader("If-Match") String expectedCommitSha,
            @RequestBody UpdateTemplateDraftCommand command) {
        return ApiResponse.success(registry.updateDraft(
                templateKey, draftId, stripQuotes(expectedCommitSha), command));
    }

    @DeleteMapping("/{templateKey}/drafts/{draftId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteDraft(
            Authentication authentication,
            @PathVariable String templateKey,
            @PathVariable String draftId) {
        registry.deleteDraft(templateKey, draftId, actor(authentication));
        return ApiResponse.success(null);
    }

    @PostMapping("/{templateKey}/validate")
    public ApiResponse<TemplateValidationResult> validate(
            @PathVariable String templateKey,
            @RequestParam String draftId) {
        return ApiResponse.success(registry.validateDraft(templateKey, draftId));
    }

    @PostMapping("/{templateKey}/preview")
    public ApiResponse<TemplatePreviewResult> preview(
            @PathVariable String templateKey,
            @RequestParam String draftId,
            @RequestBody(required = false) Map<String, Object> sampleData) {
        return ApiResponse.success(registry.previewDraft(templateKey, draftId, sampleData));
    }

    @PostMapping("/{templateKey}/test-send")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<AcceptedNotification> testSend(
            @PathVariable String templateKey,
            @Valid @RequestBody CreateNotificationCommand command) {
        CreateNotificationCommand test = new CreateNotificationCommand(
                command.idempotencyKey(), command.sourceService(), command.sourceEventId(),
                command.eventType(), command.correlationId(), command.causationId(), templateKey,
                command.locale(), command.category(), command.priority(), command.scheduledAt(),
                command.expiresAt(), true, command.recipient(), command.channels(), command.payload());
        return ApiResponse.accepted(notificationService.accept(test));
    }

    @PostMapping("/{templateKey}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TemplatePublicationResult> publish(
            Authentication authentication,
            @PathVariable String templateKey,
            @RequestBody PublishRequest request) {
        return ApiResponse.success(registry.publishDraft(templateKey, request.draftId(),
                request.expectedCommitSha(), actor(authentication)));
    }

    @GetMapping("/{templateKey}/versions")
    public ApiResponse<List<TemplateVersionSummary>> versions(
            @PathVariable String templateKey,
            @RequestParam Channel channel,
            @RequestParam String locale) {
        return ApiResponse.success(registry.findVersions(templateKey, channel, locale));
    }

    @GetMapping("/{templateKey}/versions/{version}")
    public ApiResponse<TemplateRegistry.TemplateDocument> version(
            @PathVariable String templateKey,
            @PathVariable String version,
            @RequestParam Channel channel,
            @RequestParam String locale) {
        return ApiResponse.success(registry.getTemplateVersion(templateKey, channel, locale, version));
    }

    @GetMapping("/{templateKey}/versions/diff")
    public ApiResponse<VersionDiff> diff(
            @PathVariable String templateKey,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam Channel channel,
            @RequestParam String locale) {
        return ApiResponse.success(new VersionDiff(
                registry.getTemplateVersion(templateKey, channel, locale, from),
                registry.getTemplateVersion(templateKey, channel, locale, to)));
    }

    @PostMapping("/{templateKey}/rollback")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TemplatePublicationResult> rollback(
            Authentication authentication,
            @PathVariable String templateKey,
            @RequestBody VersionActionRequest request) {
        return ApiResponse.success(registry.rollback(templateKey, request.channel(), request.locale(),
                request.version(), actor(authentication)));
    }

    @PostMapping("/{templateKey}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> archive(
            Authentication authentication,
            @PathVariable String templateKey,
            @RequestBody VersionActionRequest request) {
        registry.archive(templateKey, request.channel(), request.locale(), actor(authentication));
        return ApiResponse.success(null);
    }

    @PostMapping("/{templateKey}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> restore(
            Authentication authentication,
            @PathVariable String templateKey,
            @RequestBody VersionActionRequest request) {
        registry.restore(templateKey, request.channel(), request.locale(), actor(authentication));
        return ApiResponse.success(null);
    }

    private String actor(Authentication authentication) {
        return authentication == null ? "system" : authentication.getName();
    }

    private String stripQuotes(String value) {
        return value == null ? null : value.replace("\"", "");
    }

    public record DraftCreateRequest(
            String templateKey,
            TemplateRegistry.TemplateContent content) {
    }

    public record PublishRequest(String draftId, String expectedCommitSha) {
    }

    public record VersionActionRequest(Channel channel, String locale, String version) {
    }

    public record VersionDiff(
            TemplateRegistry.TemplateDocument from,
            TemplateRegistry.TemplateDocument to) {
    }
}
