# work-service

Task management, assignments and submission history

## Purpose

Tasks, assignees, submissions, status history, and authorization are
implemented with PostgreSQL persistence.

## Endpoints

- GET /api/health – health check
- `/api/v1/phases/{phaseId}/tasks` – task creation and listing
- `/api/v1/tasks/{taskId}` – task updates and status transitions\n- `/api/v1/tasks/{taskId}/summary` – task summary with counts and latest status\n- `/api/v1/tasks/{taskId}/history` – task status history\n- `/api/v1/tasks/{taskId}/assignees` – assignee management\n- `/api/v1/tasks/{taskId}/attachments` – private task attachment metadata and storage keys\n- `/api/v1/tasks/{taskId}/submissions` – submission history\n\nBinary object upload/download integration and workspace management remain future work.

