# Notification security

- External/admin/customer endpoints use the shared JWT and role/ownership checks.
- Internal APIs use a constant-time comparison of `X-Internal-Token` and are not gateway-routed.
- Recipient email, phone, and Web Push subscription values are encrypted with AES-256-GCM using `NOTIFICATION_RECIPIENT_ENCRYPTION_KEY`.
- Secrets and Git credentials come from environment variables or a secret manager.
- Template keys, locales, branch names, and resolved paths are allowlisted and path-normalized.
- Rendering rejects missing required variables, unknown variables, unsupported helpers, blocks, and triple-brace expressions.
- Rendered HTML is sanitized with an allowlist. Preview is returned for a sandboxed iframe.
- Logs contain public IDs, correlation IDs, template commit/version, channel, status, and failure code; they do not contain recipient addresses, payloads, tokens, or rendered bodies.

Rotate the recipient key through an explicit re-encryption job before removing the previous key. Protect `main` and version tags in the external template repository and grant the service account only the permissions needed to fetch, push draft branches, merge, and create tags.
