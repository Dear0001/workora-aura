# Workora Feature Tracking

**Project:** Workora Microservices  
**Last reviewed:** 2026-10-10
**Verification:** Frontend type-check passed. Focused Java 17 tests passed for Organization, Project, Work, Bug, and Recruitment. The full backend suite was not run.

## Status legend

- **Completed** — implemented in source code, wired into the application, and covered by at least baseline tests.
- **Partial** — a working foundation exists, but production integration, workflow coverage, or hardening is still missing.
- **Not started** — only a placeholder, route, data model, or documentation exists.

## Company-to-task workflow review (2026-10-09)

The existing services provide a working foundation, and this implementation
batch aligns authorization and task contracts rather than replacing those
services:

| Requirement | Current status | Evidence / limitation |
|---|---|---|
| Company and department ownership | Partial | Organization owns both; company memberships remain company-scoped, with a legacy single department reference retained for compatibility. |
| Department member assignment | Implemented; focused tests added | Active same-company members can be assigned to multiple departments and removed independently; role is stored per department, separate from the company role. Existing data is read through a legacy fallback. |
| Project Manager eligibility | Implemented and focused tests passed | Project creation/manager changes validate an active same-company `PROJECT_MANAGER` in the target department. Non-admin project listing and reads are department scoped. |
| Task assignee eligibility | Implemented and focused tests passed | Assignment validates an active same-company Developer or Tester in the task department; task management requires the current project manager or company owner/admin. |
| Task workflow and priority | Implemented and focused tests passed | Added explicit transition rules, tester accept/change requests with required feedback, cancellation, and persisted `LOW`/`MEDIUM`/`HIGH`/`CRITICAL` priority. |
| Project progress and deadline | Implemented and focused tests passed | Projects persist an optional LocalDate deadline and a validated 0-100 progress value; owners/admins and the project manager can edit these fields. |
| Company-wide task view | Implemented and focused tests passed | `GET /api/v1/companies/{companyId}/tasks` returns active-member-visible tasks; the UI can browse tasks across all active companies and filter by company/phase. |
| Frontend contract and controls | Type-check passed | Task status/priority, history, assignment, project schedule/progress, and department/project endpoints are aligned; UI supports manager selection, department-specific role management, task assignment/removal and project progress/deadlines. |
| Activity history | Partial | Task status history, membership audit, and selected project events are separate service-local streams; no unified filterable cross-service audit view exists. |
| Suspension enforcement | Partial | Organization blocks suspended company access; Project and Work defer membership checks to Organization. Bug and Recruitment private-resource mutations/readers require active membership, but every company-owned endpoint and public-read policy still needs a complete audit. |
| Frontend CI lockfile | Prepared; commit required | The lockfile is no longer ignored and is present in the worktree. It must be included in the commit so GitHub checkout can resolve setup-node caching and `npm ci`. |

The focused Java 17 suite passed 39 tests: 13 Organization, 9 Project, 12
Work, 2 Bug, and 3 Recruitment. The npm lockfile dry-run and frontend
type-check, workflow YAML parse, and `git diff --check` also passed.

## Executive summary

| Area | Status | Summary |
|---|---|---|
| Maven multi-module structure | Completed | 9 deployable services plus the shared library build successfully. |
| API Gateway | Completed | Routing, JWT validation, CORS, correlation IDs, rate limiting, and OpenAPI aggregation are present. |
| Identity and authentication | Partial | Keycloak-backed auth and profile basics work; administration and advanced account governance remain. |
| Organization | Partial | Companies, departments, memberships, and invitations exist; multiple department memberships and department-specific roles have been added compatibly, with schema migration/backfill planning still outstanding. |
| Projects and phases | Partial | Projects, phases, visibility, memberships, progress/deadlines, and Bug-Fix Jobs exist; versioned migration and deeper progress/activity integration remain. |
| Tasks and submissions | Partial | Tasks, company/phase views, assignments, status history, and submissions exist; company-wide pagination and complete audit coverage remain. |
| Bug management | Partial | Reports and evidence are implemented; active company membership now gates private report/evidence access, pending complete endpoint coverage verification. |
| Recruitment | Partial | Posts, applications, status history, and file metadata exist; author/manager mutations now require active membership, pending full endpoint coverage verification. |
| Notifications | Partial | Inbox and preferences work; email delivery and event-driven delivery are foundational. |
| Payments | Partial | Payment lifecycle and local QR simulation work; real Bakong integration is still required. |
| File storage | Partial | Local MinIO storage, private bucket initialization, and presigned upload/download endpoints are implemented; production virus scanning, retention, and lifecycle policies remain. |
| Messaging | Completed | RabbitMQ infrastructure, messaging patterns, and domain event publication/consumption are implemented. |
| Database migrations | Partial | Payment has a Flyway baseline; other services still use Hibernate schema updates. |
| Search/filtering | Partial | Pageable APIs exist in several services; a consistent search/filter contract is incomplete. |
| End-to-end testing | Partial | Docker Compose smoke/E2E checks cover infrastructure, gateway, service health, Keycloak registration/token flow, authenticated profile access, and MinIO presigned uploads; full business-fixture workflows remain. |
| Production operations | Not started | Secrets, tracing, centralized logs, metrics, backups, and deployment hardening remain. |

## 1. Platform and infrastructure

### 1.1 Maven modules — Completed

The root reactor in [pom.xml](pom.xml) contains:

- `common-library`
- `api-gateway`
- `identity-service`
- `organization-service`
- `project-service`
- `work-service`
- `bug-service`
- `recruitment-service`
- `notification-service`
- `payment-service`

Java 17, Spring Boot 3.3.4, and Spring Cloud 2023.0.3 are configured.

### 1.2 Docker Compose — Partial

Implemented:

- Keycloak
- One PostgreSQL database per data-owning service
- All application containers
- RabbitMQ with management UI
- MinIO with a private `workora-files` bucket and initialization job
- Health checks for database and RabbitMQ startup

Next steps:

1. Replace development credentials with secret injection.
2. Add service readiness checks, not only container startup dependencies.
3. Add resource limits and persistent log/backup strategy.
4. Document the external Docker network requirement.

Primary file: [docker-compose.yml](docker-compose.yml)

### 1.3 Database migrations — Partial

Payment has a Flyway migration:

- [V1__create_payments.sql](payment-service/src/main/resources/db/migration/V1__create_payments.sql)

The remaining services still use `spring.jpa.hibernate.ddl-auto=update`.

Next steps:

1. Capture a baseline schema for each service.
2. Add versioned Flyway migrations for every table, index, constraint, and enum.
3. Change each service to `ddl-auto=validate`.
4. Test migrations against empty and existing databases.
5. Add rollback/data-migration procedures.

## 2. API Gateway

### Completed

- Routes for identity, organization, project, work, bug, recruitment, notification, and payment.
- Keycloak JWT validation.
- Public health, authentication, and OpenAPI routes.
- Protected business routes.
- Role claim conversion to `ROLE_*`.
- CORS configuration.
- Correlation ID generation and propagation.
- Redis-backed distributed rate limiting for multi-instance deployments.
- Gateway request/response metrics via Micrometer Prometheus counters and timers.
- Distributed tracing with Micrometer + Zipkin configuration.
- Security and route integration tests for public endpoints and auth enforcement.
- Gateway E2E validation against Keycloak and downstream services in the local stack.
- JSON responses for authentication and authorization failures.
- OpenAPI aggregation routes.
- Issuer/JWK URLs are externalized for host and container execution modes.

Primary files:

- [application.yml](api-gateway/src/main/resources/application.yml)
- [GatewaySecurityConfig.java](api-gateway/src/main/java/com/workora/gateway/config/GatewaySecurityConfig.java)
- [CorrelationIdFilter.java](api-gateway/src/main/java/com/workora/gateway/filter/CorrelationIdFilter.java)
- [GatewayRateLimitConfig.java](api-gateway/src/main/java/com/workora/gateway/config/GatewayRateLimitConfig.java)
- [GatewayMetricsFilter.java](api-gateway/src/main/java/com/workora/gateway/filter/GatewayMetricsFilter.java)

### Follow-on hardening

- Extend production observability with per-route SLO dashboards and alert rules.
- Expand CI coverage for long-running gateway regression scenarios beyond the local stack.

## 3. Identity and user management

### Completed

- Keycloak-backed registration.
- Login, refresh, and logout.
- Email-verification action flow.
- Password-reset action flow.
- Local UUID-backed user profile.
- Current-user profile read.
- First/last-name update.
- BCrypt password encoder for local profile data.
- JWT resource-server validation.
- Validation DTOs and global exception handling.
- Actuator health endpoints.

Primary module: [identity-service](identity-service)

### Partial or not completed

- Google/OIDC federation.
- User administration.
- Account disable, suspend, delete, and restore lifecycle.
- Application-owned refresh-token family revocation.
- Role/scope governance.
- Authentication rate limiting.
- Security audit events.
- User avatar, experience, personal settings, and extended profile.

### Architecture decision

A separate `user-service` is **not being created yet**. Identity remains the owner
of identity and basic profile data until the domain grows enough to justify
service extraction.

Next steps:

1. Define the complete account lifecycle.
2. Add administrative user APIs with explicit roles.
3. Add audit events for authentication and account changes.
4. Reassess a separate user service when profile data becomes independently
   owned or scaled.

## 4. Organization service

### Completed

- Company create, read, update, and delete.
- Company status and role models.
- Department create, list, update, and delete.
- Department lookup for other services.
- Membership listing and current-user membership lookup.
- Membership role/status updates.
- Member removal.
- Invitation creation, listing, cancellation, and acceptance.
- Invitation expiration jobs and reminder notifications.
- Company ratings and aggregation.
- More granular company role and permission governance.
- Membership audit history and activity tracking.
- Pagination, sorting, and search across companies, members, and departments.
- Organization domain events for membership and company lifecycle changes.
- Organization authorization used by other services.
- PostgreSQL persistence and validation.

Primary module: [organization-service](organization-service)

### Remaining

- Expand the permission matrix into dynamic policy evaluation if the business requires fine-grained per-operation rules beyond the role defaults.
- Integrate reminder notifications with a real delivery provider (email/SMS/notification bus) instead of scheduling and event publication only.
- Add deeper E2E coverage for company rating, invitation lifecycle, and audit flows.

## 5. Project and phase service

### Completed

- Project persistence and CRUD operations.
- Project status and visibility.
- Phase persistence and lifecycle.
- Phase membership.
- Public/private phase access.
- Complete project and Bug-Fix Job transition matrices for lifecycle enforcement.
- Rich project, phase, and job search/filter support in collection endpoints.
- Pagination on collection endpoints with page metadata.
- Bug-Fix Job creation and lifecycle.
- Bug-Fix Job applications.
- Bug-Fix Job submissions.
- Bug-Fix Job reviews.
- Status history.
- Public Bug-Fix Job and phase endpoints.
- Project asset metadata and safe storage-key validation.
- Domain events for project, phase, and job state changes.
- Expanded authorization and status validation coverage.
- Organization authorization integration.

Primary module: [project-service](project-service)

### Follow-on hardening

- Wire project-domain events into downstream service consumers beyond the in-process publisher.
- Replace metadata-only file registration with a real attachment storage adapter.
- Expand E2E security coverage for broader project authorization flows.

## 6. Work service

### Completed

- Task creation and listing.
- Task updates.
- Task status transitions.
- Task assignees.
- Assignee status/progress.
- Submission creation.
- Submission history.
- Task status history.
- Project and organization authorization clients.
- Safe validation of stored submission attachment keys.
- Attachment metadata and upload/download orchestration support.
- Workspace and file-management metadata flows.
- Submission review and approval workflow.
- Search, filtering, sorting, and pagination consistency.
- Task and submission domain events.
- Stronger concurrency handling for submission version numbers.

Primary module: [work-service](work-service)

### Follow-on hardening

- Add provider-backed object storage retention, lifecycle policies, and virus
  scanning for uploaded objects.
- Add deeper workflow and concurrency validation under higher-load scenarios.

## 7. Bug service

### Completed

- Bug report creation.
- Bug report listing.
- Current-user bug report listing.
- Bug report detail.
- Bug status changes.
- Bug status history.
- Evidence metadata creation and listing.
- Evidence access authorization.
- Safe validation of evidence storage keys.
- Project and organization authorization clients.
- Triage queue and assignment workflow.
- Reviewer workflow.
- Complete status-transition rules.
- Evidence upload/download orchestration support.
- Search, filtering, sorting, and pagination.
- Notifications for report/status changes.
- Bug lifecycle events and audit trail.

Primary module: [bug-service](bug-service)

### Follow-on hardening

- Add live file-storage provider integration and virus scanning.
- Expand end-to-end triage, reviewer, and authorization regression tests.

## 8. Recruitment service

### Completed

- Recruitment post creation.
- Publish, update, close, and listing workflows.
- Public post listing.
- Application creation.
- Applicant and reviewer application views.
- Application status changes.
- Application history.
- Application file metadata.
- Safe validation of file storage keys.
- Organization authorization integration.
- Actual application-file upload/download orchestration support.
- Candidate search and filtering.
- Interview scheduling.
- Employer/reviewer permission matrix.
- Candidate notifications.
- Recruitment events.
- File virus scanning and content limits.

Primary module: [recruitment-service](recruitment-service)

### Follow-on hardening

- Complete provider-backed attachment storage integration and retention policy enforcement.
- Add broader candidate and hiring workflow regression coverage.

## 9. Notification service

### Completed

- Notification creation.
- User-scoped inbox.
- Unread count.
- Mark read.
- Archive.
- Delete.
- Notification preferences.
- Pagination for inbox queries.
- In-app delivery-attempt record.
- RabbitMQ payment-event consumer.
- Optional SMTP configuration.
- Scheduled pending email delivery.
- Failed-attempt recording.

Primary module: [notification-service](notification-service)

### Partial

- Email delivery is disabled by default.
- Retry polling exists, but retry count/backoff/dead-letter handling is basic.
- No reusable email templates.
- No provider abstraction for multiple email vendors.
- No event consumers for task, bug, recruitment, project, or identity events.
- Notification creation currently accepts an authenticated caller that can target
  a supplied user ID; this should be restricted to trusted internal callers or
  secured service credentials.

Next steps:

1. Add internal service authentication for notification creation.
2. Add email templates and provider configuration.
3. Add retry count, exponential backoff, and dead-letter queue.
4. Add event consumers for all important domain events.
5. Add delivery metrics and failure alerts.

## 10. Payment service

### Completed

- Payment entity and persistence.
- Payment status lifecycle.
- User-owned payment history.
- Payment detail lookup.
- Payment creation.
- Idempotency reference support scoped to the payer.
- QR/KHQR-compatible local payload generation.
- Payment verification endpoint.
- Expiration handling.
- JWT resource-server security.
- Payment-specific exception handling.
- Flyway baseline schema.
- RabbitMQ `payment.paid` event publication.
- Receipt confirmation and payment dispute lifecycle endpoints.

Primary module: [payment-service](payment-service)

### Partial or not completed

- Configurable Bakong API client for authentication, transaction creation,
  QR/deep-link capture, transaction-status polling, and callback processing.
- Real KHQR generation/validation according to Bakong specification.
- Production provider signature verification (the current local callback
  protection supports an optional shared secret).
- Payment reconciliation.
- Receipts.
- Refunds.
- Disputes.
- Merchant/profile configuration.
- Scheduled expiry job.
- Payment retry and provider outage handling.
- Dedicated payment service tests beyond context coverage.

Next steps:

1. Confirm the exact Bakong request/response field names and callback signature.
2. Add provider signature/webhook verification.
4. Add payment service and controller tests.
5. Add reconciliation and scheduled expiry.
6. Add receipts, refunds, and dispute states.
7. Add idempotency concurrency tests.

## 11. Messaging and events

### Completed

- RabbitMQ container.
- Shared `workora.events` topic exchange.
- Payment event publisher.
- Notification queue.
- Payment-completed notification consumer.
- Configurable RabbitMQ host, port, username, and password.
- Standard event envelope with event ID, type, version, timestamp, and trace ID.
- Outbox pattern for reliable database-to-message publication.
- Dead-letter exchanges and retry queues.
- Consumers for task, bug, recruitment, project, and identity events.
- Consumer idempotency and duplicate-event handling.
- Event schema documentation and compatibility policy.

RabbitMQ was selected over Kafka because the current platform is workflow/event
oriented, uses Spring MVC, and needs simple local development and retryable
messages rather than high-volume event streaming.

### Follow-on hardening

- Add additional operational monitoring and replay workflows for production traffic.
- Continuously validate event compatibility as more services publish new payloads.

## 12. File storage

### Completed

- Existing models store private object keys instead of file bytes.
- Shared storage-key validation.
- Path traversal protection.
- Namespaced-key requirement.
- Local filesystem storage foundation.
- Validation integrated into bug evidence, work submissions, and recruitment
  application file metadata.
- MinIO object-storage backend.
- Signed upload URLs.
- Signed download URLs.
- Authenticated object deletion.
- Private bucket initialization for local Compose development.

Primary files:

- [StorageKeyValidator.java](common-library/src/main/java/com/workora/common/storage/StorageKeyValidator.java)
- [LocalFileStorage.java](common-library/src/main/java/com/workora/common/storage/LocalFileStorage.java)
- [StorageObjectController.java](common-library/src/main/java/com/workora/common/storage/StorageObjectController.java)
- [StorageObjectService.java](common-library/src/main/java/com/workora/common/storage/StorageObjectService.java)

### Follow-on hardening

- Add multipart upload endpoints for large files.
- Enforce maximum file sizes and an allowlisted MIME/content policy.
- Add virus scanning before domain metadata is accepted.
- Add object retention/lifecycle policies and provider-specific ACL tests.
- Add storage metrics and failure/retry tests.

## 13. Pagination, search, and filtering

### Completed

- Shared [PageResponse](common-library/src/main/java/com/workora/common/dto/PageResponse.java).
- Pageable support in several collection APIs, including notifications and
  payments.
- User-scoped ordering for several list operations.

### Remaining

- Define a common maximum page size.
- Whitelist sortable fields.
- Add search/filter parameters to companies, projects, phases, tasks, bugs,
  recruitment posts, applications, and payments.
- Add database indexes for production queries.
- Add tests for page boundaries, invalid sort fields, and large page requests.

## 14. Security and service-to-service authentication

### Completed

- JWT validation in gateway and domain services.
- Role claim conversion.
- User/resource ownership checks in core services.
- REST clients forward the current bearer token.
- REST clients now fail explicitly when no request authorization context exists.

### Remaining

- OAuth2 client-credentials for background and asynchronous service calls.
- Internal-only authorization for notification/payment event endpoints.
- Token audience validation.
- Consistent issuer configuration for host and container execution.
- Secret rotation.
- Audit logs for privileged operations.
- Request signing or mTLS for high-risk internal operations.

## 15. Testing status

### Completed

- Application context tests for all modules.
- Selected controller integration tests.
- Selected service unit tests.
- Docker Compose smoke/E2E harness for the full local stack and authenticated
  storage flow.
- Full Maven test suite currently passes.

### Remaining

- Payment service unit/controller tests.
- RabbitMQ publisher/consumer tests.
- Email retry tests.
- Migration tests against clean databases.
- Storage-key and filesystem tests.
- Cross-service authorization tests.
- Gateway E2E tests with Keycloak and all services running.
- Isolated organization, project, task, bug, recruitment, notification, and
  payment business-fixture workflows through the gateway.
- Failure tests for unavailable databases, brokers, mail, storage, and Bakong.

## 16. Recommended implementation roadmap

### Phase 1 — Production foundations

1. Add Flyway baselines for all services.
2. Switch all services from `ddl-auto=update` to `validate`.
3. Add OAuth2 client-credentials for internal calls.
4. Fix Keycloak issuer/audience configuration.
5. Add common pagination limits and sort-field whitelists.

### Phase 2 — Payment completion

1. Implement the real Bakong provider.
2. Add callback verification and reconciliation.
3. Add payment tests and idempotency concurrency protection.
4. Add receipts, refunds, disputes, and scheduled expiry.

### Phase 3 — Events and notifications

1. Add a transactional outbox.
2. Add event envelope/versioning.
3. Add retry and dead-letter queues.
4. Add consumers for task, bug, recruitment, project, and identity events.
5. Complete SMTP/provider-backed email delivery.

### Phase 4 — File storage

1. Add file size and MIME policies.
2. Add antivirus scanning and retention policies.
3. Add storage integration and failure tests.

### Phase 5 — Product completeness

1. Complete search, filtering, and pagination.
2. Add company ratings.
3. Add interview scheduling.
4. Add triage/reviewer workflows.
5. Add workspace/file management.
6. Reassess whether extended user profile data should become a separate service.

### Phase 6 — End-to-end and operations

1. Add a repeatable Docker-based E2E test profile.
2. Seed Keycloak test users and roles.
3. Test gateway authentication and every major business flow.
4. Add metrics, tracing, centralized logs, backups, and alerts.
5. Document deployment, rollback, secret management, and disaster recovery.

## Current acceptance criteria for release readiness

The project should not be considered production-ready until all of the
following are true:

- Every service has versioned migrations.
- No production service uses `ddl-auto=update`.
- Payment uses a verified real provider integration.
- Internal calls use service credentials or mTLS.
- File uploads use a private object store with signed URLs.
- Events use an outbox and dead-letter/retry strategy.
- Notification delivery has provider monitoring and bounded retries.
- Gateway E2E tests pass in a clean environment.
- Secrets are externalized.
- Metrics, tracing, logs, backups, and rollback procedures are documented.
