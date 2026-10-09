# project-service

Projects, phases, Bug-Fix Jobs and access control

## Purpose

Projects, phases, phase membership, public visibility, and Bug-Fix Jobs are
implemented with persistence and authorization.

## Endpoints

- GET /api/health – health check
- `/api/v1/projects` – project management
- `/api/v1/projects/{projectId}/phases` – phase management
- `/api/v1/phases/{phaseId}/members` – phase membership
- Bug-Fix Job, application, submission, review, and public-read endpoints

Advanced workflow automation, event publishing, and file storage remain future
work.
