-- Preserve the exact rendered content and template revision used by each delivery.
-- Automatic and manual retries reuse these columns instead of resolving the
-- latest active template again.
ALTER TABLE notification_deliveries
    ADD COLUMN template_commit_sha VARCHAR(64) NULL AFTER provider_message_id,
    ADD COLUMN template_version VARCHAR(40) NULL AFTER template_commit_sha,
    ADD COLUMN rendered_subject VARCHAR(200) NULL AFTER template_version,
    ADD COLUMN rendered_html LONGTEXT NULL AFTER rendered_subject,
    ADD COLUMN rendered_text LONGTEXT NULL AFTER rendered_html;
