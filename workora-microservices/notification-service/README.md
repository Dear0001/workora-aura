# notification-service

Inbox notifications, preferences and email delivery

## Purpose

Inbox notifications and notification preferences are implemented with
pagination, read/archive state, and delivery-attempt metadata.

## Endpoints

- GET /api/health – health check
- `/api/v1/notifications` – create, list, read, archive, and delete
- `/api/v1/notifications/unread-count` – unread count
- `/api/v1/notification-preferences` – read and update preferences

Provider-backed email delivery, event consumers, retry workers, and templates
remain future work.

Notification creation is restricted to trusted internal callers. Configure the
shared secret through `NOTIFICATION_INTERNAL_SERVICE_TOKEN` and send it as the
`X-Internal-Service-Token` header when creating notifications from other services.
