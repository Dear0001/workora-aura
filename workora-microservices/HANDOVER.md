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

## Prior deliverable

The importable Postman Collection remains at `postman/Workora.postman_collection.json` (198 routes with DTO-shaped examples and explicit `http://localhost:8080` URLs).

## Dashboard integration

- The frontend calls `GET /api/dashboard/api/v1/dashboard/summary` and expects metrics, status breakdown, a 14-day task chart, recent activity, and active project cards.
- `DashboardAggregationService` aggregates user-scoped companies/projects/phases/tasks/bugs/members and unread notifications, forwarding the caller JWT to each owning service.
- The separate admin overview uses `GET /api/dashboard/api/v1/dashboard/admin/summary` and aggregates eight owner-service count APIs. Each metric is read from its owning service database and requires the Keycloak realm ADMIN role.
- The existing response DTO is preserved. It calculates active projects, today's due tasks, open bugs, unique team members, phase completion, task status counts, a 14-day task chart, recent project/task/bug activity, and up to five active project summaries.
- Downstream failures are allowed to surface; they are not converted into success-shaped empty data.
- Compose supplies the internal service URLs and starts the dashboard after its API dependencies.

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
