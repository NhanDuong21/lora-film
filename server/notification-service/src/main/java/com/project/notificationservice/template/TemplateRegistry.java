package com.project.notificationservice.template;

import com.project.notificationservice.domain.NotificationTypes.Category;
import com.project.notificationservice.domain.NotificationTypes.Channel;
import com.project.notificationservice.domain.NotificationTypes.TemplateStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface TemplateRegistry {

    TemplateDraft createDraft(CreateTemplateDraftCommand command);

    TemplateDraft updateDraft(
            String templateKey,
            String draftId,
            String expectedCommitSha,
            UpdateTemplateDraftCommand command);

    TemplateDraft getDraft(String templateKey, String draftId);

    TemplateDocument getPublishedTemplate(String templateKey, Channel channel, String locale);

    TemplateDocument getTemplateVersion(String templateKey, Channel channel, String locale, String version);

    List<TemplateSummary> findTemplates(TemplateSearchCriteria criteria);

    List<TemplateVersionSummary> findVersions(String templateKey, Channel channel, String locale);

    TemplateValidationResult validateDraft(String templateKey, String draftId);

    TemplatePreviewResult previewDraft(
            String templateKey,
            String draftId,
            Map<String, Object> sampleData);

    TemplatePublicationResult publishDraft(
            String templateKey,
            String draftId,
            String expectedCommitSha,
            String actorPublicId);

    TemplatePublicationResult rollback(
            String templateKey,
            Channel channel,
            String locale,
            String targetVersion,
            String actorPublicId);

    void deleteDraft(String templateKey, String draftId, String actorPublicId);

    void archive(String templateKey, Channel channel, String locale, String actorPublicId);

    void restore(String templateKey, Channel channel, String locale, String actorPublicId);

    RegistryHealth health();

    record TemplateContent(
            String displayName,
            String description,
            Category category,
            Channel channel,
            String locale,
            Map<String, VariableDefinition> variablesSchema,
            Map<String, Object> sampleData,
            String subject,
            String htmlContent,
            String textContent) {
    }

    record VariableDefinition(String type, boolean required) {
    }

    record CreateTemplateDraftCommand(
            String templateKey,
            String actorPublicId,
            TemplateContent content) {
    }

    record UpdateTemplateDraftCommand(
            String changeSummary,
            TemplateContent content) {
    }

    record TemplateDraft(
            String templateKey,
            String draftId,
            String branch,
            String baseCommitSha,
            String commitSha,
            TemplateDocument document) {
    }

    record TemplateDocument(
            String templateKey,
            String displayName,
            String description,
            Category category,
            Channel channel,
            String locale,
            TemplateStatus status,
            Map<String, VariableDefinition> variablesSchema,
            Map<String, Object> sampleData,
            String subject,
            String htmlContent,
            String textContent,
            String commitSha,
            String version,
            Instant committedAt) {
    }

    record TemplateSearchCriteria(
            String query,
            Category category,
            Channel channel,
            String locale,
            Boolean archived) {
    }

    record TemplateSummary(
            String templateKey,
            String displayName,
            Category category,
            Channel channel,
            String locale,
            TemplateStatus status,
            String publishedVersion,
            String commitSha,
            Instant committedAt) {
    }

    record TemplateVersionSummary(
            String version,
            String gitTag,
            String commitSha,
            String author,
            Instant committedAt,
            String changeSummary) {
    }

    record TemplateValidationResult(boolean valid, List<String> errors, List<String> warnings) {
    }

    record TemplatePreviewResult(
            TemplateValidationResult validation,
            RenderedTemplate rendered,
            String commitSha) {
    }

    record RenderedTemplate(String subject, String htmlContent, String textContent) {
    }

    record TemplatePublicationResult(
            String templateKey,
            Channel channel,
            String locale,
            String commitSha,
            String version,
            String gitTag,
            boolean rollback) {
    }

    record RegistryHealth(boolean available, String provider, String branch, String headCommit, String message) {
    }
}
