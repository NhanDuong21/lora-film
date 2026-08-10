# External Git template registry

The private repository is the source of truth. It is not this application repository and its working tree must not be under `src/main/resources`.

Each published template has this layout:

```text
templates/{category}/{template-key}/{channel}/{locale}/
  manifest.json
  subject.txt
  body.html
  body.txt
```

`manifest.json` contains metadata, a strict variable schema, and safe sample data. Draft edits occur on isolated branches. Every update supplies the expected draft commit SHA; stale writes return HTTP 409. Publishing validates and previews, creates a no-fast-forward merge on `main`, then tags the version as `{template-key}/{channel}/{locale}/v000001`. Rollback copies an old version into a new commit and creates the next version tag; history is never rewritten.

Only published content may be used by delivery workers. If Git is unavailable, the health indicator is down and new renders fail safely. Redis invalidation is best effort and Git remains authoritative.

Create the external repository with the Git provider's normal project setup,
protect `main`, and grant the service account only the read/write permissions it
needs. Configure the URI, branch, work directory and credentials through the
`NOTIFICATION_TEMPLATE_GIT_*` variables documented in the service's
[`application.example.properties`](../../server/notification-service/src/main/resources/application.example.properties).
The application repository does not contain a registry bootstrap script and
must not seed production template bodies.
