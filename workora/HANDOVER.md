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

## Company invitation role alignment

- Goal: fix the company invitation form returning HTTP 500 when it submits a frontend-only company role.
- Evidence: organization-service logs show JSON could not deserialize `MANAGER`; backend `CompanyRole` supports OWNER, ADMIN, PROJECT_MANAGER, DEVELOPER, TESTER, and BUG_HUNTER. The invite service separately rejects granting OWNER by invite.
- Approved UX: offer Project Manager, Developer, Bug Hunter, Tester, and Admin, with no implicit default; user must choose.
- Planned: type invitation role values to valid company role literals, align the invite form labels/options and request payload, and show backend validation errors accurately.
- [x] Use backend-supported invite roles with an explicit role choice.
- [x] Update frontend role request types and invitation modal options.
- [x] Change invitation copy to accurately report pending invitation creation rather than email delivery.
- Validation: `npm run type-check` and targeted `next lint --file src/app/company-management/components/InviteMemberModal.tsx` passed; `git diff --check` passed.
- Note: organization-service currently creates pending invitations but does not send invitation email; email delivery remains outside this fix.

## Incoming company invitation inbox

- User reported that a successful company invite is invisible to its intended recipient.
- Root cause: creation persists a pending record but does not send email or create a notification; the signed-in recipient pending-list and accept APIs already exist but the frontend did not use them.
- [x] User approved an incoming invitations panel on Notifications, an accept action using the existing API, and a pending count in navigation.
- [x] User approved returning the company name for pending invitations so invitees can identify the sender company.
- [x] Add pending-invitation API typing/client, render/accept UI, and sidebar count.
- [x] Add company name to recipient-scoped pending invitation response; validate the service and frontend.
- Validation passed: `npm run type-check`, targeted lint for Notifications/sidebar/organization API (no warnings), and `git diff --check`.
- Invitation badge combines unread notifications and pending invitations; successful acceptance dispatches a same-page event so the sidebar count refreshes. No email, acceptance token, or fake notification is used by the frontend.

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

## Company, department, project, task and activity tracking review

### Remaining full-spec implementation continuation (2026-10-10)

- User requested autonomous continuation through the remaining company-to-task workflow gaps.
- Current continuation addresses production readiness: a versioned schema migration/backfill for department roles, durable cross-service activity capture/query, and a full suspension/access-policy audit.
- Chosen activity design: domain services publish allowlisted audit events to RabbitMQ; Dashboard persists and queries events in its own database. Never include credentials, bearer tokens, or sensitive personal data.
- Preserve service ownership, backward compatibility for existing memberships, and fail-closed authorization. Do not start Compose services or mutate local databases.
- Last verified baseline: frontend type-check and npm lockfile dry-run passed; focused Java 17 tests passed for Organization, Project, Work, Bug, and Recruitment.
- Concrete migration, implementation, and validation results will be recorded below as implementation proceeds.

- The implementation review confirms existing company/department CRUD, memberships and invitations, company membership audit, project/phase CRUD and project manager assignment, task assignment/submissions and task status history. Platform Admin remains a separate global role.
- The task UI/API contract now uses backend task statuses and priorities, loads real status history, supports task assignment/removal for authorized project managers and company owners/admins, and asks for feedback when requesting changes. Project creation supports selecting an active Project Manager from the same department.
- Department member management supports multiple departments and editing each department-specific role without changing the company-wide role. Existing legacy department assignments remain visible as a compatibility fallback.
- The backend validates same-company, same-department Project Manager eligibility and task assignees as active Developers/Testers. Project reads/lists are department scoped for non-owner/admin members.
- Project, Work, Bug, and Recruitment organization clients consume department role assignments. Bug report/evidence access and recruitment author/manager mutations require current active company membership; suspended or revoked access no longer succeeds solely because the caller created the resource.
- `npm run type-check`, npm lockfile dry-run, `git diff --check`, and CI workflow YAML parsing passed. Focused Java 17 tests passed: Organization 13, Project 7, Work 10, Bug authorization 2, Recruitment authorization 3.
- The shared GitHub Actions backend job now includes focused tests for Organization, Project, Work, Bug, and Recruitment authorization.
- The frontend lockfile is no longer ignored and exists in the worktree. It must be included in the eventual commit before GitHub Actions can resolve the `setup-node` cache path and run `npm ci`.
- Department membership now has an additive entity/table and role endpoints; a versioned schema migration and backfill are still needed before production rollout. Do not claim all legacy department links have been migrated.
- Projects now persist an optional deadline and a manager-maintained 0-100 progress value, with edit controls and progress bars. The detail/list due-date display uses date-only formatting to avoid timezone shifts.
- The task view now lists accessible tasks across all active companies, with company/phase filters; the backend company-task endpoint requires active company membership and returns only tasks the caller can view. Phase/project context is loaded in bounded batches.
- A unified cross-service activity stream and endpoint-by-endpoint suspension/public-read policy audit remain incomplete.
- Updated `API-ENDPOINTS.md` with department assignment/update request shapes and role values, plus member lookup routes and current task status/priority semantics.

## Frontend popup and form visibility audit (2026-10-10)

### Goal and scope

- Audit all frontend click-triggered three-dot menus, dropdowns, popovers, modals, confirmation dialogs, and forms for clipping, stacking, viewport placement, and outside-click behavior.
- Apply focused fixes in the frontend only; preserve business logic, API integrations, permissions, and existing designs.
- Check layouts, stacking contexts, overflow containers, responsive positioning, and shared popup patterns. Do not change backend or the existing MinIO-related worktree changes.

### Status and validation

- [x] User approved full frontend audit and fixes.
- [x] Record this handover before implementation.
- [x] Inventory popup/dialog patterns and identify reusable fixes and isolated issues.
- [x] Implement and validate relevant fixes.
- [x] Run frontend type-check and lint; exercise feasible popup interactions and record browser/test limitations.

### Implementation and validation results

- Added the shared portal-based `FloatingMenu` for three-dot actions, dashboard quick actions, colleague filters, and task status selection. It positions against the trigger, flips/clamps to the viewport, follows scroll/resize, closes on outside pointer input or Escape, and avoids click-through/propagation issues.
- Migrated the project, task, bug, recruitment, department, member, colleague-filter, task-detail, and dashboard menus while preserving their existing actions and one-open-menu state. Added an accessible label to the dashboard quick-action trigger, whose visible text is hidden at small widths.
- Raised affected modal overlays above floating menus and constrained long modal panels to the dynamic viewport with internal scrolling. Existing image-upload fallback behavior in company forms is preserved.
- Browser checks passed for the dashboard quick-action menu: it opened, remained within the browser-reported viewport, closed on outside click, and its New Project link navigated to `/projects/new`. The Company Management Create Company form opened and its scrollable panel remained within the viewport.
- Browser viewport tooling reported 450px wide even after requesting 360px, so this run validates the narrowest reported width rather than a true 360px viewport. Populated row/card three-dot action flows could not be exercised because the local page had no company/project records and no live API data.
- `npm run type-check` passed. `git diff --check` passed. Project-wide `npm run lint` was attempted and fails on existing broad lint/format violations; no mass formatting was applied.
- Synthetic browser auth values were removed after the checks. The previously approved MinIO and company image-upload changes remain untouched.

## Authenticated workflow testing continuation (2026-10-10)

### Goal and scope

- Continue the user-approved browser workflow: inspect existing records and available management features, then create a company, department, project, and tasks only through the UI.
- Sign-in succeeded, but the authenticated page crashed because the persisted login profile lacked optional name fields and `Topbar` called `charAt` on `undefined`.
- User approved a focused frontend fix for missing profile display values, followed by resuming the workflow. Do not record credentials or tokens here; do not bypass server authentication, authorization, or form validation.

### Status

- [x] User approved the UI profile-handling fix and workflow continuation.
- [x] Make authenticated navigation resilient to a profile with missing name fields.
- [x] Verify the UI recovers; inspect existing company/project/task records before creating anything.
- [x] Complete or document blockers in the company → department → project → task workflow.
- [x] Run focused frontend type-check and `git diff --check`.

### Results

- Updated the topbar and sidebar to handle absent profile names without throwing or displaying `undefined`; the login success toast also has a name-free fallback. `npm run type-check` and `git diff --check` passed.
- A stale browser API route mock had been intercepting earlier navigation and writes. Removed the page/context routes and synthetic auth state, then signed in once against the actual application UI. The dashboard loaded and displayed the stored account profile; signed out through the UI at the end and confirmed auth state was cleared.
- Existing data was inspected before further writes: the account already belongs to **Foreign Trade Bank Of Cambodia**, with an existing **Engineering** department and a Developer member. Projects and tasks are currently empty.
- The UI blocks a second company on the current plan and explicitly requires an upgrade. No plan change was made. An earlier test-company create against the stale mock displayed a malformed success toast, but that response was intercepted; after clearing the mock and reloading, it was absent and did not persist.
- User clarified to use the existing company and create the department, project, and tasks instead of creating another company. In **Foreign Trade Bank Of Cambodia**, created **Software Development** department (`2a814d86-5c66-4935-ace8-6fb4801ecf7a`), **Workora Demo Project** (`2540b12e-3ea1-4e26-b731-a830222bf728`), and its **Development** phase (`09ee63dd-e225-475f-971c-111c510bd889`).
- Created three draft, medium-priority tasks in that phase: **Implement dashboard overview** (`fac5e80b-230a-4bf7-84ca-38ba946e6585`), **Add project phase navigation** (`7266de79-e5f9-4158-8ed1-5899be795f2f`), and **Add project workflow smoke tests** (`028f737c-472e-41f2-9156-cbc90b4fba7b`). The user is not prompted to invite/select another task assignee through the create form; all tasks remain in DRAFT.
- The department, project, phase, and all three tasks were visible after refresh. The browser was left on the task list filtered to the new phase, authenticated via the user's normal UI session.

## Task-row assignment action (2026-10-10)

### Goal and scope

- Expose task assignment from the task list's vertical action menu to users the backend authorizes, and update the list/detail state after assignment.
- Reuse existing Work API assignee endpoints and Organization department-member lookup; do not weaken or duplicate server authorization as an enforcement mechanism.
- Verified backend rules: company OWNER/ADMIN can manage assignments; a PROJECT_MANAGER can do so only when assigned to that project and department. Candidates must be active Developer or Tester members of the task department. Backend already enforces these checks.
- Existing task detail page already has an assignment selector and uses the same APIs, but the task-list row menu has no assignment action.

### Status

- [x] User approved the frontend implementation and handover update.
- [x] Add task-row permission-aware Assign Task action and eligible-member selection/save UI. The list action is shown to company owners/admins and the project manager assigned to that department's project; the backend remains authoritative.
- [x] Reflect successful assignments in the task list and prefer assignee names/emails on the task detail page.
- [x] Run frontend type-check, `git diff --check`, and browser validation of the owner's menu action and viewport-visible assignment dialog.
- The assignment dialog successfully loaded for the new Software Development department but reported no unassigned active Developers or Testers. No member was available, so an actual assignment and post-save refresh could not be exercised without adding/inviting an eligible user.
- Focused Next lint was attempted on the two task files but reports extensive Prettier formatting violations in those files; standalone ESLint CLI is also unavailable because this project has no ESLint 9 flat config. No broad formatting changes were made.

## Task form attachments and edit flow (2026-10-10)

### Goal and scope

- Add file selection/removal to task creation and editing, save task fields through the existing Work API, upload bytes through the shared presigned MinIO flow, and link attachments using the existing task attachment endpoint.
- Add a manager-authorized Edit Task action/form, since the task list currently has creation but no editing UI and the backend already exposes a manager-authorized task PATCH endpoint.
- Keep task assignment behavior intact. Backend assignment rules and task-update authorization were reviewed and already enforce company owner/admin or assigned project manager access; assignee eligibility remains active Developer/Tester membership in the task department.
- Correct the detail-page attachment client contract: MinIO upload is a presigned PUT; Work attachment creation expects JSON metadata, not multipart form data. Reuse existing attachment listing/removal and add presigned download links.
- Preserve task changes if a file upload/link step fails, report partial success explicitly, and validate supported file types and sizes in the UI.
- Secure task files end to end: use task-scoped Work-service upload/download URL endpoints, block the generic storage upload/download/delete routes for the `tasks` namespace, and delete objects only after the backend uploader/manager check.
- Enforce the supported file extensions and 25 MB per-file maximum server-side by checking MinIO object metadata before accepting attachment metadata.

### Status

- [x] User approved the frontend scope.
- [x] User approved focused backend attachment upload/download/removal authorization after inspection found generic storage routes were not task-scoped.
- [x] Implement create/edit task file selection, validation (images, PDF, TXT, LOG, MP4, MOV, ZIP; 25 MB per file), upload/link, and partial-failure handling. Successfully saved task fields remain saved if an attachment upload fails.
- [x] Add manager-gated Edit Task menu action/form. Existing Assign Task action remains and uses the backend's existing owner/admin or assigned-project-manager authorization and eligible department-member checks.
- [x] Correct task attachment API use: obtain task-authorized presigned PUT URLs, then link JSON metadata; add authorized task attachment downloads and authorized storage cleanup on removal.
- [x] Block generic storage upload/download/delete routes for the `tasks` namespace to prevent bypassing task-scoped authorization.
- [x] Match frontend file types and 25 MB cap with backend extension and actual-object-size validation.
- [x] Browser checks: owner sees Edit Task and Assign Task; create/edit forms expose file input; selected files can be removed; unsupported `.exe` and 26 MB files are rejected with visible errors.
- [x] `npm run type-check`, task-file validator Prettier check, and `git diff --check` passed. Focused Work-service tests passed (11 tests) with Java 17.
- Full persisted upload/download and assignment-save verification remains untested in the browser: the local UI showed an API `401`, and the task department currently has no eligible active Developers/Testers. Menu/form/validation behavior was tested without saving records.
- Focused `next lint` was attempted but the task UI files report Prettier violations across existing long-form files; no whole-file formatting was applied to avoid unrelated churn.

## Company, project, and task authorization alignment

### Approved scope

- Preserve existing company `OWNER` and `ADMIN` capabilities and existing department/project role schema.
- Align project creation and project mutation controls to the signed-in user's company and department scope.
- Hide project archive/delete actions from project managers; only company owners/admins may archive projects or change project-manager assignments.
- Exclude each task's persisted creator from assignee options and validate selection before submitting.
- Add a task delete action only when backed by an Owner/Admin-authorized Work-service endpoint.
- Do not introduce roles, schema changes, or unrelated UI changes.

### Status

- [x] User approved the audited cross-stack scope.
- [x] Align project/task action visibility and task assignee selection with backend authorization; company, department, and member management actions are also gated.
- [x] Add Owner/Admin-only task archival through the Work API. Archiving hides tasks without deleting task records or dependent data.
- [x] Frontend `npm run type-check` passed; targeted diagnostics on changed frontend files reported no errors; `git diff --check` passed.
- [x] Focused Java 17 backend tests passed: Organization 23, Project 13, and Work 21 tests (57 total).
- [ ] No services were rebuilt or deployed; perform deployment/rebuild separately when requested.

## Frontend search input icon spacing (2026-10-10)

### Goal and scope

- Make the gap from leading search/filter icon to input text consistent across all frontend search fields while preserving current height, width, borders, colors, and responsive layout.
- Search fields were found in the topbar, projects, tasks, bug reports, recruitment, company member and department-role management, colleague profiles, manager decisions, and activity timeline.
- Add one shared search-input padding utility and apply it to all icon-leading search inputs; do not change unrelated inputs or search behavior.

### Status

- [x] User approved the shared spacing fix.
- [x] Added `.input-search` with 40px left padding and applied it to all audited icon-leading search inputs without changing existing field heights, widths, colors, or behavior.
- [x] Migrated topbar global search, project/task/bug/recruitment filters, company member and department-role search, colleague profiles, manager decisions, and activity timeline search.
- [x] Frontend type-check and `git diff --check` passed; no diagnostics in changed files.
- [x] Browser check at 360px and 1280px confirmed consistent 40px text inset, 14px icon-to-text gap, and vertically centered icon.

## Keycloak email verification return flow

### Approved goal and scope

- Send successful Keycloak verification back to the actual frontend authentication route and show an application-owned success state with a clear sign-in action.
- Preserve the existing pending-email notice and polling workflow; do not move verification tokens into frontend URLs or attempt to switch unrelated browser tabs.
- Rely on Keycloak's supported `redirect_uri` after verification rather than the custom theme script's brittle English-anchor matching.
- Keep Keycloak as the authority for verification and preserve existing login/registration behavior.

### Status

- [x] User approved the focused frontend/backend fix and server-enforced resend throttling.
- [x] Handle `/?status=verified` in the authentication page and show an app-owned success state with a sign-in action; clear the status query after reading it.
- [x] Preserve the pending notice and add a visible 60-second resend countdown matching backend enforcement.
- [x] Frontend `npm run type-check` and both repositories' `git diff --check` passed.
- [x] Browser verified `/?status=verified` renders the success state, clears the query, and **Continue to sign in** opens the login form.
- Local route check confirmed `/` returns HTTP 200 and `/login` returns HTTP 404, verifying the prior redirect target was invalid. A real email link was not sent or consumed in this validation.

## Cross-tab email verification notification

### Approved scope

- After the Keycloak verification tab returns to `/?status=verified`, signal same-origin tabs with `BroadcastChannel`, falling back to a short-lived `localStorage` event.
- The signal contains no email, verification session token, or authentication credential and is only a prompt for the original tab to re-check the backend.
- The original pending tab updates only after the existing session endpoint confirms `VERIFIED`; retain periodic polling and the verification-tab success/sign-in fallback.
- Do not attempt to switch focus or close tabs opened by email clients.

### Status

- [x] User approved this focused cross-tab improvement.
- [x] Publish a token-free completion signal through `BroadcastChannel`, with `storage` event fallback; subscribe to both transport mechanisms in the pending tab.
- [x] Trigger immediate re-check of the existing verification session, while retaining 5-second polling as fallback. The pending tab only shows verified after the API reports `VERIFIED`.
- [x] Stop polling after `EXPIRED` to avoid repeated Keycloak lookups, but resume one authoritative check when the cross-tab completion signal arrives.
- [x] Frontend `npm run type-check` and both repositories' `git diff --check` passed.
- [x] Browser tested two same-origin tabs: loading `/?status=verified` in one delivered the expected channel message to the other. Existing success-page behavior and sign-in fallback remain.
- Cross-tab transport was tested independently of a real Keycloak-issued verification action; no real email or account verification was exercised.

## Canonical Keycloak identity alignment

### Approved scope

- Keep Keycloak as the only credential, verification, access-token, refresh-token, and logout authority; retain the existing frontend auth entry point and API flow.
- Add an additive, unique Keycloak subject link for application profiles and expose that canonical subject as the frontend identity ID, matching other services' use of JWT `sub`.
- Preserve existing profile primary keys, business records, and memberships. Do not delete or rewrite user records; reconcile a legacy profile only after a valid Keycloak token proves the subject and matching email.
- Make successful login and profile recovery fail closed for unverified or mismatched identities; never trust a client-provided ID/email as proof.
- Verify source-level causes and report remaining live action-token diagnosis limits; do not expose action tokens or credentials.

### Status

- [x] User approved the non-destructive Keycloak-subject linkage and authentication consistency fix.
- [x] Add the nullable unique Keycloak subject link without replacing profile IDs; expose Keycloak `sub` in auth/profile responses and link legacy profiles only after validated-token identity and email matching.
- [x] Reject unverified identities before returning login/refresh tokens or serving profile endpoints; revoke the refresh session on unverified login, and have the frontend use the backend's `emailVerified` value.
- [x] Update login/refresh response types and add focused backend tests for canonical linking, identity conflicts, unverified login, profile reads/updates, verification polling, and admin-candidate linkage.
- [x] Frontend `npm run type-check` passed. Focused Java 17 identity-service tests passed: 17 tests, 0 failures/errors.
- The repository-wide frontend lint remains failing on formatting violations across existing modified UI files; linting only the touched auth helper also reports pre-existing formatting in that file. No unrelated formatting sweep was applied.

## Workora authentication UI and email theme redesign

### Approved scope

- Retain Workora's existing native login/registration entry point and existing backend/Keycloak auth responsibilities.
- Improve the auth screens and make password-reset instructions match Keycloak's email action-link flow; do not ask users to enter an unsupported reset token.
- Keep the existing pending-verification session polling, resend cooldown, success redirect, and token-free cross-tab notification behavior.
- No identity/API contract, realm/client security setting, redirect policy, or SMTP credential changes.

### Status

- [x] User approved the focused frontend and Keycloak theme redesign.
- [x] Update responsive/accessibility-aware frontend auth UX and Workora Keycloak login/action/error styles.
- [x] Add branded HTML and plain-text Keycloak verification email templates using Keycloak-generated links and actual expiry placeholders.
- [x] Run focused frontend checks, validate Keycloak theme resource structure, and capture a browser preview.
- Validation passed: `npm run type-check`, targeted `next lint` on the four changed auth components, and `git diff --check`. Browser preview confirmed the Workora login and password-reset screens render; earlier responsive checks confirmed the registration layout has no horizontal overflow at mobile width. Keycloak login theme and its CSS/logo resources rendered successfully. No live registration, password-reset request, verification email send, or action-token completion was performed.
- Existing profile primary keys and business relationships remain untouched. The new nullable `keycloak_user_id` column and unique constraint are added through the service's existing Hibernate `ddl-auto:update`; legacy rows remain unlinked until a successful verified Keycloak login/profile request proves the subject and matching email. Ambiguous conflicts fail closed and require manual review; no automatic merge or cleanup was run.
- Source confirms Keycloak generates action-token URLs and the email theme passes the generated link through. The exact cause of the previously observed `Invalid username or password` page cannot be distinguished from an expired/consumed action token or Keycloak login error without safely reproducing it with a fresh link and inspecting its result; no verification token was requested or logged. No live test registration was sent.
