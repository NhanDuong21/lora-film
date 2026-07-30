# Notification APIs

All responses use the service API envelope. Administrator endpoints require an authenticated admin JWT.

Template routes under `/api/v1/admin/notification-templates` support list/filter, published detail, draft create/read/update/delete, validation, preview, test send, publish, version list/detail/diff, rollback, archive, and restore. Draft updates and publish requests carry an expected commit SHA; conflicts return 409.

Operations routes under `/api/v1/admin/notifications` provide dashboard metrics, paged logs, request detail, delivery retry, and dead-letter views.

Customer routes under `/api/v1/notifications` provide a paged inbox, unread count, mark-one-read, and mark-all-read. Ownership comes from the JWT principal, never a request query parameter.

Internal routes under `/api/v1/internal/notifications` provide single/batch accept, status and delivery lookup, and cancellation. They require `X-Internal-Token` and are not exposed by the gateway.

Actuator publishes health, metrics, and Prometheus data. Git registry health is part of readiness.
