# work-service

Task management, assignments and submission history

## Purpose

Tasks, assignees, submissions, status history, and authorization are
implemented with PostgreSQL persistence.

## Endpoints

- GET /api/health – health check
- `/api/v1/phases/{phaseId}/tasks` – task creation and listing
- `/api/v1/tasks/{taskId}` – task updates and status transitions\n- `/api/v1/tasks/{taskId}/summary` – task summary with counts and latest status\n- `/api/v1/tasks/{taskId}/history` – task status history\n- `/api/v1/tasks/{taskId}/assignees` – assignee management\n- `/api/v1/tasks/{taskId}/attachments` – private task attachment metadata and storage keys\n- `/api/v1/tasks/{taskId}/submissions` – submission history\n\nBinary object upload/download integration and workspace management remain future work.
- `DELETE /api/v1/tasks/{taskId}` – Owner/Admin-only archive that hides the task while preserving its records and files
- `/api/v1/tasks/{taskId}/summary` – task summary with counts and latest status
- `/api/v1/tasks/{taskId}/history` – task status history
- `/api/v1/tasks/{taskId}/assignees` – assignee management; the task creator cannot be assigned
- `/api/v1/tasks/{taskId}/attachments` – private task attachment metadata and storage keys
- `/api/v1/tasks/{taskId}/submissions` – submission history

Task archiving retains bug-report references and all task history/files. Archived
tasks are omitted from active lists and cannot be loaded or modified.
