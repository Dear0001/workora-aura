# Handover: Workora dashboard and development tooling

## Goal

Implement an authenticated dashboard summary that aggregates domain data through owning-service APIs, and make dev seed fixtures usable by a selected Keycloak account.

## Approved scope

- Seed tooling path: `tools/dev/`.
- Seeds must be opt-in, clearly labeled dev-only, and never run automatically on startup.
- Use synthetic deterministic identifiers and data; never read `.env` files or embed secrets.
- Target only the local development Compose PostgreSQL databases; do not execute seeds against databases.
- Cover tables and relationships using the actual entity schema and payment migrations.
- Dashboard aggregation must use existing service APIs and caller JWT authorization; do not query another service's database directly.
- Seed runner may accept a synthetic or existing Keycloak subject UUID; fixtures remain local dev-only and never create Keycloak accounts.
- Preserve unrelated pre-existing changes in this repository.
- For the approved dashboard-only local workflow, MinIO must be optional without changing the default full-stack Compose startup; file-storage features may remain unavailable in this mode.

## Status

- [x] User requested dev seed data under `workora-microservices/tools/dev/`.
- [x] Update this handover before seed implementation.
- [x] Inventory service schemas and seed dependencies.
- [x] Implement deterministic, rerunnable dev-only seeds for each applicable database table.
- [x] Validate shell syntax, transaction structure, safe listing, and opt-in runner behavior without executing against databases.
- [x] Make `all` stop at the first failed database seed.
- [x] Align seed runner Compose project selection with `start-all-db.sh`.
- [x] User approved implementing live dashboard aggregation through existing service APIs.
- [x] User approved a `DEV_SEED_USER_ID` override for linking seed fixtures to an existing signed-in Keycloak user.
- [x] Implement dashboard aggregation through the owning services' authenticated APIs.
- [x] Expose downstream service URLs and startup dependencies in Compose configuration.
- [x] Implement and document the seed user UUID override without modifying the synthetic identity row.
- [x] Add an active non-owner membership and phase membership so dashboard member metrics have sample data.
- [x] Run focused dashboard service tests and build; do not execute seed SQL against databases.
- [x] Add a documented dashboard-only Compose override that lets dashboard dependencies start without MinIO while preserving their database/readiness dependencies.
- [x] Replaced unavailable published MinIO images with a local source build after Docker Hub and the old Quay tags both proved unavailable.

## Prior deliverable

The importable Postman Collection remains at `postman/Workora.postman_collection.json` (198 routes with DTO-shaped examples and explicit `http://localhost:8080` URLs).

## Dashboard integration

- The frontend calls `GET /api/dashboard/api/v1/dashboard/summary` and expects metrics, status breakdown, a 14-day task chart, recent activity, and active project cards.
- `DashboardAggregationService` aggregates user-scoped companies/projects/phases/tasks/bugs/members and unread notifications, forwarding the caller JWT to each owning service.
- The separate admin overview uses `GET /api/dashboard/api/v1/dashboard/admin/summary` and aggregates eight owner-service count APIs. Each metric is read from its owning service database and requires the Keycloak realm ADMIN role.

## Platform Admin role boundary and GitHub Actions

- Platform Admin is the global Identity `ADMIN` role and is independent of company membership. `CompanyRole` remains limited to company-scoped roles; it does not contain or grant the platform role.
- The shared repository CI workflow is at `../.github/workflows/ci.yml`. Its backend job uses Java 17 and runs focused admin authorization, membership, project authorization, and task workflow tests from this repository.
- Keep platform role approval and signed-token authorization separate from organization membership permissions.
- Validation: all 22 selected backend tests passed using Java 17; workflow YAML parsed successfully and `git diff --check` passed.

## Company, department, project, task and activity tracking review

### Remaining full-spec implementation continuation (2026-10-10)

- User requested autonomous continuation through the remaining company-to-task workflow gaps.
- Planned implementation focus: backward-compatible department membership and department roles, cross-service activity capture/query, company suspension enforcement, persisted project schedule/progress, and company-wide task views.
- Do not bypass service ownership or assume events are a durable audit store without verifying broker and consumer configuration. Preserve existing API behavior where possible.
- Last verified baseline: focused Java 17 CI tests across Identity, Organization, Project, Work, and Dashboard passed; frontend type-check and `npm ci --ignore-scripts --dry-run` passed.
- Record concrete implemented scope, migrations, validation, and unavoidable service-infrastructure limitations here before handoff.

- Existing foundations: Organization owns companies, departments, company memberships/invitations, and membership audit; Project owns company/department projects, phases, manager IDs, and selected domain events; Work owns task/assignee/submission and task-status-history records.
- The Platform Admin `ADMIN` role is global and remains independent of company roles.
- This batch aligns task DTOs/UI with the backend; adds task priority, tester accept/change-request transitions and cancellation; validates current project managers and task assignees against active same-company/department membership; and scopes non-owner/admin project access by department.
- Organization now exposes a company-scoped active-member lookup, requiring the caller to be an active member of that same company. The work/project services use it instead of trusting arbitrary UUIDs.
- Frontend `npm run type-check`, `npm ci --ignore-scripts --dry-run`, and `git diff --check` passed. Focused Organization, Project, and Work backend tests passed in the Java 17 Maven container after a truncated Maven Central transfer was retried. The full backend suite was not run.
- The frontend lockfile is no longer ignored and is available in the worktree. Include it in the eventual commit for the shared CI workflow's `setup-node` npm cache and `npm ci` to work from a GitHub checkout.
- The legacy single `departmentId` remains on a company membership for compatibility. An additive `DepartmentMembership` relation now supports multiple departments and department roles, but existing links are exposed through a read fallback rather than a completed versioned backfill migration.
- Task status history and membership audit are service-local and incomplete as a unified platform activity feed. Project progress/deadline data and company-wide task views remain unimplemented. Project/Work checks defer to active Organization membership; Bug and Recruitment private-resource mutations now check active company membership. A complete audit of every endpoint and public-read policy remains.
- The existing response DTO is preserved. It calculates active projects, today's due tasks, open bugs, unique team members, phase completion, task status counts, a 14-day task chart, recent project/task/bug activity, and up to five active project summaries.
- Downstream failures are allowed to surface; they are not converted into success-shaped empty data.
- Compose supplies the internal service URLs and starts the dashboard after its API dependencies.

### Department role and authorization continuation (2026-10-10)

- Added `DepartmentMembership`/`DepartmentRole` as an additive Organization model, department-scoped role assignment/update routes, and `MemberResponse.departmentMemberships`. Existing `departmentId` and company roles remain intact; legacy assignment fallback is not a substitute for a versioned migration/backfill.
- Project and Work Organization clients now consume department membership roles. Project Manager eligibility, project department visibility, task assignee eligibility, and Developer/Tester status transitions use department-scoped roles while retaining legacy compatibility.
- Bug report/evidence reads now require active membership even for the original reporter; Project Manager reviewer rights are evaluated against current department membership. Recruitment authors must retain active company access, and department Project Managers can manage posts in their assigned departments.
- Projects now persist optional `dueDate` and 0-100 `progress`, validate input, expose changes in project event payloads, and support frontend create/edit/progress-bar views.
- Work exposes `GET /api/v1/companies/{companyId}/tasks`, filtering on active membership and per-task authorization; the Tasks UI shows accessible tasks across active company memberships, with company and phase filters. `/tasks/me` also filters out tasks whose company access has been revoked.
- CI now includes Bug and Recruitment authorization tests and setup-node points at `workora/package-lock.json`. The lockfile must be committed for GitHub Actions to resolve that cache path.
- Validation passed: 39 focused Java 17 tests (Organization 13, Project 9, Work 12, Bug 2, Recruitment 3); frontend TypeScript type-check and npm lockfile dry-run; workflow YAML parse; `git diff --check`.
- Remaining work: versioned schema migration/backfill; audit all company-scoped endpoints and public-read behavior for suspension; implement durable cross-service activity feed; add end-to-end workflow and cross-company denial tests.

### Current continuation plan (2026-10-10)

- Add a versioned Organization migration/backfill for the additive department membership relation, converting valid legacy company-member assignments to explicit department roles.
- Implement durable RabbitMQ audit events from domain services and Dashboard-owned persistence/querying. Events carry allowlisted entity IDs, action, company/department context and safe change summaries only.
- Audit company-scoped endpoints and distinguish intentionally public reads (such as published Recruitment posts) from operations that require active membership after suspension.
- Do not start Compose or touch local databases. Verify code, migration shape and focused tests in isolated test contexts.

## Seed tooling

- `tools/dev/seed-dev.sh` requires `--confirm-dev` and only targets the PostgreSQL services from this repository's `docker-compose.yml`; it does not start services. It uses `COMPOSE_PROJECT_NAME` or the same `workora-microservices-2` default as `start-all-db.sh`.
- `tools/dev/README.md` documents usage, synthetic IDs, limitations, raw-SQL caveats, matching the Compose project name, and associating domain fixtures with an existing Keycloak subject.
- `docker-compose.dashboard.yml` is an opt-in Compose override for running the dashboard and gateway without MinIO. It removes only the MinIO-init dependency for gateway-required services and preserves database/Keycloak readiness dependencies; upload/download features are unavailable in this mode. The normal Compose configuration remains unchanged.
- Per-database fixtures: `identity.sql`, `organization.sql`, `project.sql`, `work.sql`, `bug.sql`, `recruitment.sql`, `notification.sql`, and `payment.sql`.
- The approved `DEV_SEED_USER_ID` override applies only to logical user references in domain databases. The identity fixture must remain fixed, synthetic, unverified, and must never overwrite an existing identity record.
- Rows use stable synthetic IDs and explicit timestamps; each file is transactional and rerunnable. Inserts cover entity tables and element-collection tables. Project/task/bug activity dates are relative to seed time, with an example due-today task and a recent completion for the dashboard.
- The organization/project fixtures include one active synthetic developer membership and phase membership so an owner account sees a nonzero team-member count.
- Identity records are unverified synthetic fixtures with a non-credential marker; they are not Keycloak accounts. The payment fixture stays `CREATED`, and file keys are metadata only.

## Validation results

- `sh -n tools/dev/seed-dev.sh` passed.
- `sh tools/dev/seed-dev.sh --list` returned the eight supported targets without requiring Docker or connecting to a database.
- `sh tools/dev/seed-dev.sh --confirm-dev all` stops after the first failed service instead of continuing through the remaining databases.
- Compose project selection defaults to `workora-microservices-2` and can be overridden with `COMPOSE_PROJECT_NAME`.
- Dashboard-only Compose override validated with Docker Compose v5.5.1; it requires Compose 2.24 or newer and leaves the normal full-stack setup unchanged.
- `mvn -q -pl dashboard-service -am test` passed in a Java 17 Maven container; the local host JDK is Java 8.
- `docker compose --env-file /dev/null --project-name workora-microservices-2 -f docker-compose.yml config --quiet` passed. The empty env file caused expected unset-variable warnings; no `.env` contents were read.
- Static checks confirmed that all domain seed SQL references use the validated psql user-ID variable, while `identity.sql` remains a fixed synthetic record.
- Static checks confirmed eight SQL files, one `BEGIN`/`COMMIT` pair per file, and no `DROP` or `TRUNCATE` statements.
- No database was seeded or queried. `psql` and a local SQL parser are unavailable, so SQL execution-level validation remains outstanding; validate against disposable local Compose databases before relying on a fixture after schema changes.
- Rebuild/recreate `dashboard-service` after pulling these changes; no other service needs a rebuild for the dashboard aggregation code.

## Approved role-aware admin dashboard work

### Scope and constraints

- Provide a read-only, system-wide admin overview through a distinct frontend `/admin` area. The current user dashboard remains unchanged.
- Authorize with global identity `ADMIN`; do not confuse it with company membership roles.
- Expose only aggregate metrics. Preserve each service's database ownership and do not query cross-service databases directly.
- Align the Keycloak signed-token role claim with Spring Security before adding ADMIN-only API access. The current resource-server converters look for a top-level `roles` claim.
- Extend dev seeding only via an explicit option to update an existing local identity profile selected by `DEV_SEED_IDENTITY_USER_ID`; preserve `DEV_SEED_USER_ID` for Keycloak-subject domain fixtures and leave default seed behavior unchanged.
- Do not start services, pull images, or apply seeds against databases.

### Status

- [x] Confirm the Keycloak role claim contract and available owner-service aggregate metrics.
- [x] Add ADMIN-protected aggregate metrics in data-owning services and aggregation in dashboard-service, with focused tests.
- [x] Add explicit role-option handling and documentation in `tools/dev/seed-dev.sh` and `tools/dev/README.md`.
- [x] Validate targeted modules, shell behavior, and Compose/API contract; record limitations.

### Implementation and validation

- Added a shared Keycloak authority converter that accepts the standard signed-token `realm_access.roles` claim while preserving compatibility with the existing top-level `roles` claim.
- Identity, Organization, Project, Work, Bug, Recruitment, Notification, and Payment each expose `GET /api/v1/admin/metrics`, protected by `ROLE_ADMIN`, returning only an aggregate count from their own database.
- Dashboard service exposes `GET /api/v1/dashboard/admin/summary`, protects it with `ROLE_ADMIN`, and aggregates the service counts through authenticated downstream APIs. Compose supplies the added internal URLs and startup dependencies.
- The Postman collection and frontend endpoint map include the admin routes.
- `seed-dev.sh --confirm-dev --set-account-role ROLE` can update exactly one existing local identity profile selected by `DEV_SEED_IDENTITY_USER_ID`. The separate `DEV_SEED_USER_ID` still identifies domain fixture ownership. The role update does not change the Keycloak realm role; local admins must assign realm ADMIN and obtain a fresh token.
- Focused dashboard regression/aggregation, claim-conversion, and HTTP authorization tests passed. Maven compiled all changed backend modules in the Java 17 container. A full-module test attempt stopped in the existing identity application-context test because PostgreSQL was unavailable; no databases were started or modified.
- `sh -n`, seed target listing, a fake-Docker role-option test, Postman JSON validation, Compose override validation, and frontend `npm run type-check` passed. No SQL was applied to a database.
- Rebuild changed identity, organization, project, work, bug, recruitment, notification, payment, and dashboard service images before using the new admin API. The API gateway source is unchanged.

## Additional administrator capabilities (implemented with documented limits)

### Requested scope and decisions

- Extend the separate admin UI with health/Grafana, approval-gated admin provisioning, company access control, subscription administration, and activity review.
- Keep Grafana optional and externally configured; do not put Grafana credentials in browser-visible configuration.
- Preserve service ownership: identity owns account roles, organization owns company access/subscription records, and activity data remains with the service that records it.
- Keep the existing per-owner subscription schema; changing it to company-scoped billing would require a separate data migration and product decision.
- Never grant Keycloak realm ADMIN before approval. Avoid self-approval.
- Do not claim complete platform activity coverage from the existing organization membership audit table.
- Do not modify or execute local database fixtures as part of this work.

### Implementation details and validation

- `GET /api/v1/dashboard/admin/health` probes readiness endpoints for Identity, Organization, Project, Work, Bug, Recruitment, Notification, Payment, and Dashboard. Individual unreachable services return `DOWN`; one probe is bounded to a two-second connect/read timeout.
- `POST /api/v1/admin/accounts/requests` creates an invite as a regular Keycloak USER when needed and stores a pending request. `POST .../{id}/approve` rejects self-approval, updates the Identity profile, and grants Keycloak realm ADMIN only after approval; reject uses `POST .../{id}/reject`.
- Organization admin APIs support paginated company listing, suspend/reactivate, owner-user subscription updates, and filtered/paginated membership audit records. Admin page sizes are capped at 100.
- The current company suspension does not centrally disable requests that bypass Organization. The current activity repository is not a complete audit log. Subscriptions remain per owner account and do not update payment-provider charges.
- Frontend `npm run type-check` and focused lint passed. Focused Java 17 Maven tests passed for `AdminAccessRequestServiceTest`, `KeycloakServiceAdminRoleTest`, `MembershipServiceTest`, `CompanyControllerTest`, `AdminSystemHealthServiceTest`, and `AdminDashboardAggregationServiceTest`.
- Compose validation and Postman JSON validation passed; the Postman collection now contains 198 requests. The organization Spring test context was validated with disposable in-memory H2 and dummy MinIO test properties.
- A broader Identity application-context test still cannot run without PostgreSQL; no database seeds were run and no application/infrastructure services were started.
- Set frontend `NEXT_PUBLIC_GRAFANA_URL` only when an external Grafana instance is available. No Grafana service or dashboard provisioning is included.

### Status and next steps

- [x] Add dashboard-service health probes for the dashboard and eight owner services, with bounded HTTP timeouts and service/database readiness status.
- [x] Add approval requests for existing/invited admin accounts. New invitations begin as regular USER accounts; a different admin must approve before Identity and Keycloak ADMIN roles are granted.
- [x] Add organization-owned admin company status, subscription, and paginated membership-audit APIs.
- [x] Add focused tests and publish the new API routes/request examples in Postman and API docs.
- [ ] Add a durable cross-service activity event pipeline; current filtered activity endpoint contains only existing Organization membership audit events.
- [ ] Enforce suspended-company status at other domain-service boundaries; current guard is in Organization service only.
- [ ] Migrate subscriptions from owner-user scope to company scope and connect lifecycle operations to billing only after a separate migration/product decision.

## Local MinIO image registry repair (2026-10-10)

### Goal and decision

- Make local object storage runnable without substituting another S3-compatible product.
- The user explicitly chose MinIO Community built from source after the old Quay tags and Docker Hub images proved unavailable. Upstream marks Community Edition unmaintained and source-only; this is for local development, not a production recommendation.

### Scope and status

- Approved scope: update `docker-compose.yml`, add a local MinIO/MinIO Client source-build Dockerfile, and document the local startup flow.
- Preserve the existing MinIO ports, persistent volume, credentials, bucket initialization, service dependencies, and presigned URL configuration.
- Before implementation, inspect all Compose MinIO references and preserve unrelated existing worktree changes.
- Build the pinned MinIO Community `RELEASE.2024-10-13T13-34-11Z` and `mc` `RELEASE.2025-08-13T08-35-41Z` source tags into one local development image.
- [x] Replace unavailable remote MinIO image references with a locally built MinIO Community/Client image.
- [x] Validate the Dockerfile build and rendered Compose configuration; MinIO infrastructure was not started.
- Validation passed: `docker compose config --quiet`, `git diff --check`, `docker compose build minio`, and runtime `minio --version` / `mc --version` checks in disposable containers.

## Task attachment authorization (2026-10-10)

### Goal and approved scope

- Expose task-attachment download URLs only through a task-scoped Work-service endpoint that applies existing task-view authorization.
- Issue task-file upload URLs through a task-scoped Work-service endpoint and block generic storage URL issuance for the `tasks` namespace.
- Validate supported task-file extensions and verify actual MinIO object size does not exceed 25 MB before saving attachment metadata.
- Delete the stored object only after the existing uploader-or-task-manager permission check succeeds.
- Preserve task creation/update and assignment authorization. Frontend uploads continue using the shared presigned MinIO upload flow; Work-service stores only validated object keys and metadata.
- User approved this focused backend extension after review found the generic storage upload/download/delete routes do not authorize against task membership or attachment ownership.

### Status

- [x] User approved backend authorization changes.
- [x] User approved task-scoped upload URL authorization and blocking generic task-namespace upload URLs.
- [x] Add task-scoped upload/download endpoints with task-view authorization, namespace-constrain attachment keys to `tasks/`, and remove stored files only after uploader/manager authorization.
- [x] Block generic storage upload/download/delete routes for the `tasks` namespace.
- [x] Validate allowed task-file extensions at upload URL issuance and check actual object size (25 MB maximum) before saving attachment metadata.
- [x] Add focused tests for upload/download authorization, extension and size validation, attachment deletion ownership, namespace checks, and generic-route bypass prevention.
- Validation passed with Java 17: `mvn -pl work-service -am -Dtest=TaskAttachmentServiceTest,TaskStorageRouteAuthorizationTest -Dsurefire.failIfNoSpecifiedTests=false test` (11 tests passed).

## Keycloak email verification return flow

### Approved goal and scope

- Send Keycloak's verification action to the existing frontend route (`/`) with a success status, rather than the nonexistent `/login` route.
- Retain Keycloak's standard verification action and configured client redirect validation; do not introduce a custom verification mechanism or weaken email verification.
- Remove the custom theme script's brittle English-anchor auto-navigation so Keycloak's supported post-verification redirect controls the browser tab that followed the email link.
- Enforce a persistent per-account cooldown on verification-email resends while keeping the API response generic for unknown accounts.

### Status

- [x] User approved the focused fix, including server-side resend throttling.
- [x] Correct the Keycloak redirect URI to the frontend root and remove the custom auto-navigation script/styles.
- [x] Persist the last verification-email send time per user, lock the user row while enforcing a 60-second cooldown, and return HTTP 429 during the cooldown.
- [x] Keep the resend response generic for unknown accounts and avoid resending to a verified account.
- [x] Focused Java 17 identity tests passed: 8 tests across resend behavior and HTTP rate-limit status. Both repositories' `git diff --check` passed.
- Keycloak 25.0 is configured with `verifyEmail` enabled and the `workora-api` client allows the frontend origin. The realm export has no custom verification required-action provider.
- Confirmed bug: the redirect currently targets `/login?status=verified`, while the frontend defines its authentication page at `/`; that success query is not handled. The custom script redirects only if it finds an English anchor labeled “Proceed” or “Continue”.
- A verification email client opening a new tab is external browser behavior; the supported fix returns that tab to the frontend and cannot switch another tab.
- End-to-end email delivery, token expiry, and post-verification Keycloak redirect were not exercised because no test registration email was sent. No realm/client configuration change is required; the existing valid redirect pattern covers the frontend root.

## Cross-tab email verification status

### Approved scope

- Keep Keycloak as the source of truth for email verification and keep verification-session tokens out of cross-tab signals.
- Ensure the backend session endpoint returns `VERIFIED` when Keycloak confirms verification even if the frontend correlation session's 15-minute waiting window has elapsed; return `EXPIRED` only for an unverified account.
- Preserve the existing polling API and response shape.

### Status

- [x] User approved this focused frontend/backend status behavior.
- [x] Check Keycloak verification before reporting the correlation session expired; save and return VERIFIED when Keycloak confirms, even after the waiting session TTL.
- [x] Add tests for expired-but-verified and expired-and-unverified sessions.
- [x] Focused Java 17 tests passed: 10 total, including registration verification, resend cooldown, and HTTP 429 handling. Both repositories' `git diff --check` passed.
- Existing poll response shape and token hashing remain unchanged; the session token is never broadcast.

## Canonical Keycloak identity alignment

### Approved scope

- Keycloak remains the only credential, verification, and token authority. Retain the existing gateway/resource-server JWT validation and refresh/logout delegation.
- Add a unique, nullable Keycloak subject to application profile rows; preserve generated profile primary keys and all existing business records.
- Link a legacy profile only after validating a Keycloak-issued token and confirming that its normalized email matches the profile. Reject subject/email conflicts.
- Use immutable Keycloak `sub` for API identity IDs and user profile lookup, consistent with other microservices' `CurrentUser`.
- Prevent unverified Keycloak identities from completing backend login/profile access. Keep legacy credential columns for compatibility; do not delete account data in this change.
- Make registration retries and profile recovery avoid duplicate identities/profiles; compensate only for a Keycloak user created by the current failed registration request.

### Status

- [x] User approved this non-destructive cross-stack identity alignment.
- [x] Audit identity-service registration/login/refresh, JWT validation, profile endpoints, admin-candidate provisioning, and downstream use of Keycloak `sub`.
- [x] Add nullable unique `keycloak_user_id`; preserve generated profile primary keys and all business records. Auth/profile responses now expose canonical Keycloak subject IDs.
- [x] Registration and admin-candidate provisioning link the subject returned by Keycloak. Verified login/refresh uses the signature- and issuer-validated access token claims; matching legacy profiles are linked lazily and conflicting profile/subject mappings fail closed.
- [x] If Keycloak account creation succeeded but the identity profile was not saved, a retry can recover only after the submitted password authenticates and a validated token confirms the same email/subject; it reuses the existing Keycloak user and does not create another account.
- [x] Reject unverified identities on login, refresh, and profile endpoints; include `emailVerified` in auth responses. Registration explicitly sets Keycloak's `VERIFY_EMAIL` required action and addresses verification sends by exact Keycloak user ID.
- [x] Focused Java 17 identity-service tests passed: 17 tests, 0 failures/errors; frontend `npm run type-check` passed; `git diff --check` passed.
- The complete identity-service test suite was also attempted; the Spring context-load test cannot connect to the local identity PostgreSQL database because this test environment provides no datasource password. All focused auth/verification/admin-role/error-mapping tests pass.
- No existing local profile IDs, users, business records, or memberships were rewritten or deleted. Hibernate `ddl-auto:update` applies the nullable column and unique constraint; unlinked legacy profiles reconcile only after a validated token proves subject and matching email. Subject/email conflicts remain for manual investigation.
- No shared service/container was restarted and no live test account/email was created. The exact runtime cause of the reported Keycloak `Invalid username or password` page remains unconfirmed; a fresh-link test or redacted Keycloak event evidence is needed to distinguish action-token expiry/consumption from an authentication error.
- Existing database schema uses Hibernate `ddl-auto:update`; no versioned identity migration is present. On rollout, the new nullable subject column and unique index are applied without modifying existing IDs.

## Workora authentication UI and email theme redesign

### Approved scope

- Theme the existing Keycloak 25 login, required-action, and error pages to match Workora without replacing Keycloak's authentication/verification templates or changing realm/client security configuration.
- Upgrade the Workora verification email presentation with branded HTML and plain text, retaining Keycloak's genuine `{0}` action URL and configured expiration argument; do not invent support details.
- Preserve trusted redirects and do not expose action tokens outside Keycloak's generated link.

### Status

- [x] User approved the focused frontend/Keycloak UI and email-theme work.
- [x] Confirmed runtime Keycloak version is 25.0.6; realm export selects the existing `workora` login and email themes and already has email verification enabled.
- [x] Implement responsive/focus-visible theme CSS and branded verification email templates; no realm/client or SMTP changes planned.
- [x] Verify template/theme resource paths against Keycloak 25 and update with test/preview results.
- Validation passed: Keycloak 25 theme resources and login/email logo paths are present and served by the running instance; frontend type-check, targeted auth lint, and `git diff --check` passed. No live email was sent and the FreeMarker templates were not exercised through a real verification action; deploy/restart Keycloak only if the target environment requires a theme cache reload.

### Verification-result screen polish (2026-10-10)

- [x] User approved a theme-only refinement to the Keycloak screen reached from email verification; preserve Keycloak's action-token flow and realm/client settings.
- The shared runtime browser currently shows a timed-out sign-in page, not an email-verification result. Its Workora theme is active, but the danger alert and password input group retain default light styling in dark mode.
- [x] Align Keycloak alert and input-group states with Workora light/dark colors and clarify verified/expired-link messages.
- [x] Preview the available Keycloak auth state and validate theme resources; a fresh email action link is unavailable for an end-to-end verification-page preview.
- Fixed the Keycloak page container to fill the viewport (the prior theme left half the page unstyled), aligned status alerts/password controls to the selected light/dark palette, and improved wrapping for status text.
- Validation passed: browser preview of the live Keycloak registration page at the available viewport, temporary DOM-only success/error-style checks, HTTP 200 for the mounted CSS/logo, and `git diff --check`. No verification token was fabricated and no account or email action was performed; the shared page was not reached via a fresh verification email.

### Verification email template runtime failure (2026-10-10)

- [x] User approved fixing the FreeMarker error shown in the supplied Keycloak logs; preserve Keycloak's genuine verification URL and do not send a real email.
- Runtime logs show the HTML email template fails to parse because `?html` is used while Keycloak's HTML output format already enables auto-escaping. This prevents Keycloak from rendering/sending verification emails.
- [x] Remove redundant legacy HTML-escape built-ins from the HTML verification template and statically validate its escaping/interpolations without invoking email delivery.
- [x] Removed `?html` from the personalized greeting and Keycloak-generated link expressions; FreeMarker's HTML auto-escaping remains responsible for output escaping.
- Validation passed: the affected HTML template contains no legacy `?html` built-ins, retains Keycloak's generated `${link}` and expiration formatter, and `git diff --check` passed. No local FreeMarker runtime is available for a true parser/render test, and no email was sent.
- The same log excerpt also contains earlier `VERIFY_EMAIL_ERROR` events with `user_not_found`; those are separate from this template parse error and need a fresh safe link/runtime context to diagnose.

### Company invitation role deserialization failure (2026-10-10)

- Goal: stop company invites returning HTTP 500 when the frontend sends a role absent from the backend enum.
- Evidence from organization-service logs: `HttpMessageNotReadableException` / `InvalidFormatException` for `"MANAGER"`; accepted enum values were OWNER, PROJECT_MANAGER, DEVELOPER, TESTER, BUG_HUNTER, ADMIN. The invite service prevents granting OWNER via invite.
- Approved frontend behavior: expose Project Manager, Developer, Bug Hunter, Tester, and Admin, requiring an explicit choice.
- Planned: handle malformed enum/request-body values as HTTP 400 in the organization service, align frontend invitation role typing/options, and add focused tests/documentation.
- [x] Handle malformed request bodies and invalid company-role enums with HTTP 400.
- [x] Add controller tests proving unsupported roles fail with 400 and a supported invitation role creates an invitation.
- Validation: `CompanyControllerTest` passed (6 tests) via the existing Maven Java 17 image. The host only has Java 8, and the first container test attempt revealed missing optional MinIO config; rerunning with dummy MinIO-only test placeholders passed. Frontend type-check/lint and `git diff --check` passed.
- Note: service currently persists a pending invitation and returns its acceptance token in the API response; email delivery is not implemented by this endpoint.

### Incoming company invitations in the recipient UI (2026-10-10)

- User reported the invitee cannot see or accept an invitation after company invite succeeds.
- Source trace: creation stores `PENDING`; there is no invite-created notification/email. `GET /api/v1/invitations/pending` already restricts records to the authenticated JWT email, and authenticated accept-by-company/id exists.
- [x] User approved extending only the pending-invitations response with company name; recipient scope and token secrecy remain unchanged.
- [x] Add company names to the pending-only DTO (not the outgoing admin invitation DTO); cover with controller test.
- [x] Frontend exposes pending invitations and acceptance in Notifications; no notification/email backend workflow added.
- Validation passed: `CompanyControllerTest` (6 tests) proves a recipient can list an invitation with its company name and accept it. Existing Java 17 Maven image used with non-secret MinIO test placeholders. Frontend type-check and targeted lint passed.
- Pending list remains scoped to the exact email in the authenticated Keycloak token. Acceptance continues through the existing authenticated endpoint; no raw accept token is exposed or transported by the inbox.

## Company, project, and task authorization alignment

### Approved scope

- Preserve company `OWNER` and `ADMIN` authority and existing department/project role schema.
- Enforce assigned-department project creation, Owner/Admin-only project archival and manager promotion, and view-scope checks on project-member reads.
- Enforce task creator self-assignment denial in Work service using immutable `Task.createdBy`, with a clear validation error.
- Implement Owner/Admin-only task soft archival, preserving the task and dependent records.
- Restrict department and department-member reads to Owner/Admin or members assigned to that department.
- Add focused Project and Work authorization tests; do not introduce role definitions or unrelated database changes. A nullable `archivedAt` timestamp supports task soft archival.

### Status

- [x] User approved the audited cross-stack scope.
- [x] Enforce organization, project-manager, and task-assignment restrictions in owning services.
- [x] Enforce task creator self-assignment denial using persisted immutable `Task.createdBy`.
- [x] Restrict task archival to Owner/Admin and hide archived tasks without deleting dependent data.
- [x] Focused Java 17 tests passed: Organization 23, Project 13, and Work 21 tests (57 total).
- [ ] Services were not rebuilt or deployed; perform deployment/rebuild separately when requested.
