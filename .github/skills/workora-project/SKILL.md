---
name: workora-project
description: "Use when implementing, debugging, testing, or reviewing Workora frontend or backend changes, including API contracts across the Next.js app and Spring microservices."
---

# Workora Project Engineering

Use this skill for changes in either repository:

- Frontend: `workora/`
- Backend: `workora-microservices/`

For cross-stack work, inspect both implementations and verify the API contract end to end. Keep changes in the owning feature or service and avoid unrelated refactors.

## Approval and handover

- Before changing any project file, explain the intended changes and ask the user for approval. Do not edit files until they approve; approval covers only the stated scope. If the scope changes, ask again.
- After approval and before implementation, create or update `HANDOVER.md` at the root of each affected repository. For a cross-stack change, maintain a handover in each repository that will be modified.
- Keep the handover useful for continuing the work: summarize the goal and context, decisions and constraints, implementation status, relevant files or areas, validation performed, and concrete next steps. Update it as work progresses and before handing off.
- Do not put credentials, tokens, or other secrets in a handover document.

## Engineering quality

- Favor reliable, flexible, scalable, and maintainable solutions that fit existing project patterns.
- Prefer clear, cohesive changes with appropriate types, validation, error handling, and focused tests. Avoid speculative abstraction, duplication, hidden failures, and unrelated refactoring.
- Consider compatibility and likely future extension points without adding unnecessary complexity. Explain material trade-offs when asking for approval.

## Frontend

- The frontend is Next.js 15 App Router, React 19, and strict TypeScript. Routes and layouts are in `workora/src/app/`; shared components are in `src/components/`, and shared API clients, types, and auth helpers are in `src/lib/`.
- Follow the existing feature-local pattern: pages/components for UI and typed API modules under `src/lib/api/` for network calls. Keep request/response types aligned with the backend.
- Use the shared Axios client in `src/lib/api/client.ts`. It reads `NEXT_PUBLIC_API_BASE_URL` (default `http://localhost:8080`), attaches the browser-stored token, and handles refresh/retry and logout behavior. Do not introduce direct ad hoc requests in feature UI when a shared API module is appropriate.
- Auth currently uses browser `localStorage`; guard browser-only access from server-rendered code.
- Use existing Tailwind utilities and CSS-variable theme tokens such as `bg-background`, `text-foreground`, and `border-border`; reuse shared button, input, and card classes. Icons commonly use `lucide-react`; notifications use Sonner.
- Check `workora/API-ENDPOINTS.md` for documented frontend/backend contract mismatches, but verify live behavior against gateway routing and backend controllers.
- Useful commands from `workora/`:
  - `npm run type-check`
  - `npm run lint`
  - `npm run build`
  - `npm run dev` (port 3000)
- There is no evident frontend test runner configured. Next build settings may skip TypeScript and ESLint failures, so run those checks explicitly.

## Backend

- The backend is a Java 17 Maven multi-module project using Spring Boot 3.3 and Spring Cloud. Modules include `common-library`, `api-gateway`, and domain services; each application has its own `*Application.java` entry point.
- Domain boundaries: identity owns accounts/profile; organization owns companies and memberships; project owns projects/phases and bug-fix jobs; work owns tasks and submissions; bug owns reports/evidence; recruitment owns posts/applications; notification owns inbox/preferences/email; payment owns payment lifecycle/provider integration; dashboard aggregates data.
- Keep domain changes in the owning service. Follow the established controller → service → repository/entity flow, use request/response DTOs and validation, and use `CurrentUser` for caller identity.
- Services generally expose `/api/v1/...`. The gateway maps `/api/{service}/**` and strips the first two path segments. When changing or adding an endpoint, check the controller, gateway route and security configuration, shared `ApiResponse<T>` envelope, and any frontend API module that consumes it.
- Verify endpoint documentation against live code. In particular, the identity service endpoint map describes a target contract, not a complete inventory of implemented controllers.
- Each data-owning service has its own PostgreSQL database. Do not access another service's database directly; use established inter-service clients or events. RabbitMQ is used for workflow events, including payment-to-notification events.
- Keycloak provides identity and JWT validation. Preserve gateway/service authorization rules. MinIO-backed files use validated object keys and presigned upload/download flows; do not store file contents in domain records.
- Useful commands from `workora-microservices/`:
  - `mvn -pl <module> -am test` for a targeted module and its dependencies
  - `mvn test` for the full backend suite
  - `bash e2e/compose-smoke.sh` for the Compose smoke test
- Local service startup is documented by `./start-all-db.sh`, `./start-keycloak.sh`, and `./start-all-service.sh`. Confirm with the user before starting infrastructure or services if that could disrupt a shared environment.

## Cross-stack workflow

1. Identify the owning frontend feature and backend service before proposing changes.
2. Trace the request from the frontend API module through gateway routing/security to the backend controller, DTO, and service. Confirm the actual contract rather than inferring it from docs.
3. Explain the proposed scope and wait for the user's approval before changing files.
4. After approval, create or update the affected repository's `HANDOVER.md` before implementation.
5. Preserve response envelopes, auth behavior, validation, loading/error handling, and user-visible toast patterns.
6. If the contract changes, update the backend endpoint and gateway as needed, frontend API types/client, and relevant endpoint documentation together, within the approved scope.
7. Read `.env.example` for variable names only. Never read, expose, or commit `.env` credentials; never put secrets in `NEXT_PUBLIC_*` values.
8. Run focused checks for every changed stack: frontend type-check/lint and backend module tests as appropriate. Update the handover with outcomes and remaining steps; report checks that could not be run.
