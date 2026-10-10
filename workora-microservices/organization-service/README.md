# organization-service

Companies, departments, memberships, invitations, and company role governance

## Purpose

The core organization domain is implemented with PostgreSQL persistence,
validation, authorization, and REST APIs.

## Endpoints

- GET /api/health – health check
- `/api/v1/companies` – company create, read, update, and delete
- `/api/v1/companies/{companyId}/departments` – department management
- `/api/v1/companies/{companyId}/members` – membership lookup and management
- `/api/v1/companies/{companyId}/invitations` – invitation management
- `/api/v1/invitations/pending` – recipient-scoped pending invitations, including company name
- `/api/v1/invitations/accept` – accept an invitation

Company ratings, audit events, advanced role governance, and pagination remain
future work.
