# Handover: Frontend/backend API alignment

## Goal

Align frontend API clients and affected consumers with existing backend response shapes and routes for task lists, company members, unread notifications, bug evidence deletion/listing, and recruitment post deletion.

## Scope and constraints

- Frontend repository: `workora/`.
- Backend repository: `workora-microservices/` (read-only reference for this task).
- Preserve the existing API envelope and existing UI behavior where possible; do not add backend changes.
- Task and organization member endpoints return arrays, not paginated response objects. Frontend UI may continue its existing local filtering/paging.
- Keep any unrelated pre-existing backend working-tree changes untouched.

## Relevant areas

- `src/lib/api/work.ts` and task list consumers under `src/app/tasks/`
- `src/lib/api/organization.ts` and company member consumers under `src/app/company-management/`
- `src/lib/api/notifications.ts` and unread-count consumers
- `src/lib/api/bugs.ts` and bug report/evidence UI under `src/app/bugs/`
- `src/lib/api/recruitment.ts` and recruitment deletion UI under `src/app/recruitment/`

## Status

- [x] Inspect frontend/backend API evidence and identify mismatched shapes/routes.
- [x] Obtain user approval for this integration batch.
- [x] Create this handover before implementation.
- [x] Align task and company-member response types/consumers with backend array responses.
- [x] Align unread-count response type with `{ unread: number }` and use the dedicated unread-count endpoint in the sidebar.
- [x] Verify bug report listing uses `/bug-reports/me`, document the implemented route, and connect evidence deletion to the backend DELETE endpoint.
- [x] Connect recruitment post deletion helper to the backend DELETE endpoint with a null-data response type.
- [x] Run frontend type-check and lint; record results below.

## Validation and next steps

No further implementation is pending in this approved batch. Continue with the separate audit findings only after confirming scope with the user.

## Validation results

- `npm run type-check` — passed.
- `npm run lint` — did not pass: Prettier violations are reported across the app and affected files, including unrelated sections. No broad formatting changes were made.
- Targeted lint using `next lint --file ...` likewise reports Prettier violations in affected files; direct `npx eslint` is unavailable because ESLint 9 requires a flat config that this project does not provide.

## Approved admin dashboard work

### Goal and constraints

- Add a distinct read-only `/admin` overview inside this Next.js app; do not replace or alter the normal `/dashboard`.
- Gate the admin area using global identity account role `ADMIN`, not organization membership roles.
- Treat client-side role checks as UX only; the dashboard-service endpoint must enforce ADMIN server-side.
- Show aggregate-only system metrics and no individual user/company data in this first iteration.
- Use the shared Axios client and typed API module; preserve existing auth behavior and make non-admin/unknown-role access fail closed.

### Status

- [x] Inspect and align backend ADMIN claim contract with identity profile role.
- [x] Implement the admin route, dedicated layout/navigation, guard, API types/client, and overview UI.
- [x] Verify non-admin denial, admin rendering, type-check, and existing dashboard preservation.

### Implementation and validation

- `/admin` verifies the profile role through the authenticated identity `GET /users/me` API and fails closed for non-admin or unverifiable roles.
- The normal sidebar shows an Admin Overview link only for profiles with the global `ADMIN` role; company membership roles do not grant access.
- The admin UI uses the shared API client and displays aggregate counts only. A backend `403` explains that the Keycloak realm ADMIN role must also be assigned to the signed-in user.
- The API contract and authorization prerequisites are documented in `API-ENDPOINTS.md`.
- `npm run type-check` passed. No existing dashboard component or route was replaced.
- Targeted lint passed for the new admin layout, page, and API client. Linting the modified sidebar still reports Prettier issues in pre-existing, unchanged sections; those unrelated formatting changes were left alone.

## Additional administrator capabilities (implemented with documented limits)

### Requested scope and decisions

- Extend `/admin` with system health/Grafana, approved admin-account provisioning, company access controls, subscription administration, and user activity review.
- Keep Grafana externally configured; the application must not assume Grafana is installed or expose credentials in frontend configuration.
- Company subscriptions remain attached to the current owner-user data model until a deliberate company-based billing migration is approved.
- Admin privileges must remain approval-gated and Keycloak realm roles must not be granted before approval.
- Activity review must clearly state its actual event coverage; do not present membership audit events as a complete record of every platform action.
- Preserve the regular dashboard and all unrelated pre-existing changes.

### Status and next steps

- [x] Add health, account approval, company access, subscription, and activity screens to the protected `/admin` area.
- [x] Add typed API methods, navigation, optional Grafana link configuration, endpoint documentation, and Postman examples.
- [x] Run frontend type-check and focused lint successfully.
- [ ] Add a persisted cross-service activity event pipeline; current activity view is limited to Organization membership audit records.
- [ ] Enforce suspended-company status in Project, Work, Bug, Recruitment, Payment, and other domain services; current suspension only blocks Organization membership operations.
- [ ] Provide a Grafana deployment/dashboard configuration if the environment does not already operate Grafana.

### Relevant areas

- `src/app/admin/` for the separate admin overview.
- `src/components/` for the admin shell, if it remains reusable and distinct from `AppLayout`.
- `src/lib/auth.ts`, `src/lib/api/`, and shared types for role and metrics integration.
- Existing sidebar/navigation patterns under `src/components/Sidebar.tsx`.

### Implementation details and validation

- `/admin/health`, `/admin/accounts`, `/admin/companies`, `/admin/subscriptions`, and `/admin/activity` share the global ADMIN layout and client API module.
- Set optional `NEXT_PUBLIC_GRAFANA_URL` to link to an existing Grafana instance. No Grafana credentials are exposed to the browser.
- The subscription screen edits current owner-user subscription records; it does not represent company-scoped billing or provider charges.
- Company suspension is identified as limited in the UI so it is not mistaken for platform-wide enforcement.
- `npm run type-check` and focused lint over all new admin routes/API files passed. `git diff --check` passed.

## Platform Admin role boundary and GitHub Actions

- Platform Admin is a global identity role, independent of a company. Organization membership roles (`OWNER`, `ADMIN`, `PROJECT_MANAGER`, `DEVELOPER`, `TESTER`, and `BUG_HUNTER`) remain company-scoped and do not grant platform administration.
- The shared repository CI workflow is at `../.github/workflows/ci.yml`. Its frontend job runs `npm ci` and `npm run type-check` from this repository.
- Frontend lint is not currently a CI gate because the existing project-wide lint command has known baseline failures outside the admin changes.
- Validation: `npm run type-check` passed; workflow YAML parsed successfully and `git diff --check` passed.
