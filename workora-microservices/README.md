chmod +x start-all-db.sh start-keycloak.sh start-all-service.sh
./start-all-db.sh
./start-keycloak.sh
./start-all-service.sh# Workora Microservices

This project is a Spring Boot microservice skeleton built from the Workora backend plan and the Spring Boot best-practice guidance in SKILL-BACKEND.md.

## Implementation status (verified 2026-09-22)

The repository currently contains:

- A working API Gateway with routes for all eight backend services, Keycloak JWT
  validation, CORS, correlation IDs, in-memory rate limiting, consistent JSON
  401/403 responses, and Swagger aggregation routes.
- A partially implemented Identity Service with Keycloak-backed registration,
  login, refresh, logout, password-reset action requests, email-verification
  action responses, current-user profile reads, and first/last-name updates.
- A UUID-backed `users` table, BCrypt password hashing, JWT resource-server
  validation, global exception handling, validation DTOs, and Actuator health
  endpoints.
- Organization, Project, Work, Bug, Recruitment, and Notification now contain
  domain APIs, repositories, entities, authorization, and service tests.
- Payment now contains an authenticated payment lifecycle, idempotent payment
  references, QR payload generation, verification, expiry handling, receipt
  confirmation, dispute states, Flyway schema, and RabbitMQ payment events.
  The default provider is a local simulator; set `PAYMENT_PROVIDER=bakong` to
  use the configurable Bakong authentication, QR/deep-link, status-polling,
  and callback adapter.
- Docker Compose PostgreSQL containers for each data-owning service and a
  Keycloak realm import for local development. RabbitMQ is included for
  domain events. MinIO provides the local S3-compatible object store for
  project assets, task submissions, bug evidence, and recruitment files.

The feature inventory and endpoint sections below describe the target platform;
they are not claims that those APIs already exist. See each service README for
its verified implementation status.

## Database ownership rule

Each service owns its own database/schema and must keep data isolated from other services:

| Service | Database/schema |
| --- | --- |
| identity-service | identity_db |
| organization-service | organization_db |
| project-service | project_db |
| work-service | task_db |
| bug-service | bug_db |
| recruitment-service | recruitment_db |
| notification-service | notification_db |
| payment-service | payment_db |
| api-gateway | gateway_db (optional, usually stateless) |

## Recommended layer responsibility

- controller: HTTP API handling
- service: business logic and validation
- repository: database access
- entity: persistence model
- dto: request/response payloads
- mapper: entity <-> DTO conversion
- client: outbound calls to other services
- event: RabbitMQ events (the project uses RabbitMQ because the services are
  Spring MVC applications, the event volume is workflow-oriented, and the
  broker is simpler to operate locally than Kafka)
- config: security, OpenAPI, CORS, gateway, and shared configuration
- exception: global exception handling
- util: cross-cutting utilities

## Service map

- api-gateway: routing, Keycloak JWT validation, CORS, in-memory rate limiting,
  correlation IDs, JSON authentication errors, and Swagger aggregation
- identity-service: Keycloak registration/login, email-action verification, password reset, token sessions, and user profiles
- identity-service owns the current user profile. A separate user-service is
  intentionally not being added: profile/account concerns remain close to
  identity until scale or ownership boundaries justify extraction.
- organization-service: companies, memberships, departments, roles, invitations
- project-service: projects, phases, public/private visibility, Bug-Fix Jobs
- work-service: tasks, assignments, submissions, attachments, workspace visibility
- bug-service: bug reports, evidence, triage, review, lifecycle states
- recruitment-service: recruitment posts, applications, approval workflow
- notification-service: inbox, email delivery when enabled, read/archive,
  preferences, delivery-attempt retry polling, and RabbitMQ event consumption
- payment-service: authenticated payment lifecycle, local/KHQR-compatible QR
  generation, payment verification, expiry, and payment events

## Current implementation notes

- Payment has a provider abstraction. Configure `PAYMENT_PROVIDER=bakong`,
  `BAKONG_BASE_URL`, credentials/token, and an externally reachable
  `BAKONG_CALLBACK_URL` before using the bank integration. Through the gateway,
  the callback path is `/payment/api/v1/payments/callbacks/bakong`.
- File fields in bug evidence, submissions, project assets, and recruitment
  applications are private MinIO object keys. The file-owning services expose
  authenticated presigned upload/download endpoints at
  `/api/v1/storage/uploads` and `/api/v1/storage/downloads`. Domain endpoints
  continue to store only validated object keys.
- Payment has a Flyway baseline migration. The remaining services still use
  Hibernate schema updates and should receive versioned migrations before
  production deployment.

## Detailed endpoint design

See [API-ENDPOINTS.md](identity-service/API-ENDPOINTS.md) for the target endpoint contract map and its implementation-status notes.

See [AUTH-TEST-PLAN.md](identity-service/AUTH-TEST-PLAN.md) for the authentication test
cases, execution order, fraud scanning checklist, and production security
notices.

## Run locally

```bash
cd Workora-Microservices
mvn test
```

The repository currently provides POSIX shell startup scripts. On Windows, use
Docker Compose directly or run the scripts through WSL/Git Bash; there is no
`start-all.ps1` in this repository.

On Linux or macOS, start the project in three steps:

```bash
cd Workora-Microservices
chmod +x start-all-db.sh start-keycloak.sh start-all-service.sh remove-all-service.sh remove-all-workora.sh
./start-all-db.sh
./start-keycloak.sh
./start-all-service.sh
```

The scripts can be run again safely. `start-all-db.sh` starts the PostgreSQL
containers, `start-keycloak.sh` starts Keycloak, and `start-all-service.sh`
checks the application containers and source fingerprint before starting.
It rebuilds the application images only when a tracked source or build file
has changed, or when the saved state is missing.

After starting each service, the script checks its Actuator health endpoint.
If a service is not ready after two minutes, it prints the container status and
recent logs, runs Docker Compose for that service again, and retries once.
If it still fails, the script exits with the service logs so the startup error
is visible.

To automatically detect changes under `src/`, Maven files, the Dockerfile, or
the Compose file and rebuild/restart the application containers:

```bash
./start-all-service.sh --watch
```

The watcher uses polling and checks for changes every two seconds. Press
`Ctrl+C` to stop it.

To remove the application service containers and images, run:

```bash
./remove-all-service.sh
```

If database or Keycloak containers are still running, the shared Docker network
is retained because Docker cannot remove a network that is still in use. Stop
those infrastructure containers first if the network should also be removed.
The cleanup also prunes all unused images, volumes, and networks; the
`docker image prune -a -f` step removes unused images from Docker globally, not
only images belonging to Workora.

To completely reset the Workora Docker Compose project, including all
containers, images, volumes, networks, and orphan containers, run:

```bash
./remove-all-workora.sh
```

This explicitly removes the external shared Docker network as well and
permanently deletes the database volumes. It also prunes all unused Docker
images, volumes, and networks. Because the Compose file uses fixed
`container_name` values, the script also force-removes any matching Workora
containers and their attached anonymous volumes left behind by a different
Compose project name before the next startup. This prevents stale RabbitMQ
`/var/lib/rabbitmq/.erlang.cookie` permissions from being reused.

## Start infrastructure with Docker

The [docker-compose.yml](./docker-compose.yml) file defines Keycloak, one
PostgreSQL container per database-owning service, and all Spring Boot
application containers. The startup scripts above start these groups separately.

The startup scripts use the Compose project name
`workora-microservices-2` and the shared Docker network
`workora-microservices_default`.

MinIO is available locally at `http://localhost:9000` (S3 API) and
`http://localhost:9001` (console). Set `MINIO_ROOT_USER` and
`MINIO_ROOT_PASSWORD` in the ignored `.env` file; credentials are not
hard-coded in Compose. The
`workora-files` bucket is created automatically and remains private; application
clients use short-lived presigned URLs.

File upload flow:

1. Call the authenticated service endpoint
   `POST /api/v1/storage/uploads` with `namespace`, `fileName`, and
   `contentType`.
2. Upload the bytes with HTTP `PUT` to the returned `uploadUrl`.
3. Persist the returned `key` in the relevant domain request, such as bug
   evidence, a task submission, a project asset, or an application file.
4. Request `GET /api/v1/storage/downloads?key=...` when a short-lived download
   URL is needed.

Supported namespaces are `avatars`, `companies`, `projects`, `phases`, `tasks`,
`submissions`, `bug-reports`, `recruitment`, and `applications`. The bucket is
private; no permanent public object URLs are returned. Upload size limits,
content scanning, retention, and lifecycle policies remain deployment-level
hardening work.

`rebuild-all-compose.sh` starts the stack in dependency order: it keeps one
shared network, starts Redis/RabbitMQ/Zipkin once, waits for them, starts
Keycloak once and waits for it, starts and checks all PostgreSQL databases, and
then starts the application containers from the images built at the beginning.

Run the Docker Compose smoke/E2E checks with:

```bash
bash e2e/compose-smoke.sh
```

The checks start the full stack, verify Keycloak/RabbitMQ/Redis/PostgreSQL/
MinIO, probe every service and the gateway, register a disposable identity,
obtain a Keycloak token, access the authenticated profile route, and create a
presigned storage upload URL.

Alternatively, to start the same groups directly with Compose, use:

```bash
docker compose --project-name workora-microservices-2 up -d identity-db organization-db project-db work-db bug-db recruitment-db notification-db payment-db
docker compose --project-name workora-microservices-2 up -d keycloak
docker compose --project-name workora-microservices-2 build
docker compose --project-name workora-microservices-2 up -d
```

```bash
cd Workora-Microservices
docker compose up -d
```

Check database status with:

```bash
docker compose ps
```

### Local PostgreSQL credentials

Each PostgreSQL container uses these development-only defaults:

| Setting | Value |
| --- | --- |
| Username | `workora` |
| Password | `workora123` |
| Host ports | `5433` through `5440` |
| Container port | `5432` |

The database names are listed in the
[database ownership table](#database-ownership-rule). For example, Identity
Service uses `identity_db` on `localhost:5433`:

```text
jdbc:postgresql://localhost:5433/identity_db
```

The password `workora123` is only for local development. Do not reuse it in
staging or production. Replace the `POSTGRES_PASSWORD` values in
`docker-compose.yml` with deployment secrets, and update each service's
`DB_PASSWORD` environment variable to match. Existing PostgreSQL volumes keep
their original credentials; changing the Compose file alone does not change
an already-initialized database password.

Keycloak is the identity provider and is available at:

```text
http://localhost:8180
```

The local realm is `workora`, imported from
[keycloak/realm-export.json](./keycloak/realm-export.json). The configured
public client is `workora-api`. The administrator username and password come
from `KEYCLOAK_ADMIN_USERNAME` and `KEYCLOAK_ADMIN_PASSWORD` in `.env`.

The compatibility endpoints under `identity-service` delegate registration,
login, refresh, logout, and password-reset email actions to Keycloak. The
service stores only the local Workora profile and application data.

Open the administration console at:

```text
http://localhost:8180/admin/master/console/
```

If the admin login does not work after changing credentials, recreate only
the Keycloak container:

```bash
docker compose rm -sf keycloak
docker compose up -d keycloak
docker compose logs -f keycloak
```

Wait for the logs to show that Keycloak has started and the `workora` realm
has been imported. Use a private browser window if an old Keycloak cookie
remains.

The OpenID Connect issuer is:

```text
http://localhost:8180/realms/workora
```

Obtain a token from:

```text
http://localhost:8180/realms/workora/protocol/openid-connect/token
```

For local password-grant testing, create a user in the `workora` realm and
request a token:

```bash
curl -X POST "http://localhost:8180/realms/workora/protocol/openid-connect/token" ^
  -H "Content-Type: application/x-www-form-urlencoded" ^
  -d "client_id=workora-api" ^
  -d "grant_type=password" ^
  -d "username=your-user@example.com" ^
  -d "password=your-password"
```

Copy the `access_token` from the response and use it in Swagger's
**Authorize** dialog.

Send the returned access token to protected APIs as:

```http
Authorization: Bearer <access-token>
```

With PowerShell, create the authorization header explicitly after obtaining a
Keycloak token:

```powershell
$authHeaders = @{
  Authorization = "Bearer $($keycloak.access_token)"
}
```

Use `$authHeaders` on protected requests. `Invoke-RestMethod` does not send a
token automatically just because it is stored in `$keycloak`.

## Authentication manual TC reference

Use the gateway base URL:

```text
http://localhost:8080/api/identity
```

Never record real passwords, MFA codes, access tokens, or refresh tokens.
Replace them with placeholders in test evidence. Email verification and
password reset use Keycloak action links; Workora does not issue a second OTP
for either flow.

| TC | Request | Expected result |
| --- | --- | --- |
| AUTH-001 Register | `POST /api/v1/auth/register` with email, password, first name, and last name | Keycloak user and local profile are created; no password is returned |
| AUTH-003 Login | `POST /api/v1/auth/login` with Keycloak credentials | HTTP 200 with Keycloak access/refresh tokens and local profile |
| AUTH-004 Refresh | `POST /api/v1/auth/refresh` with a Keycloak refresh token | HTTP 200 with the refreshed Keycloak token response |
| AUTH-005 Logout | `POST /api/v1/auth/logout` with a Keycloak refresh token | Keycloak logout succeeds and the token is revoked |
| AUTH-006 Password reset request | `POST /api/v1/auth/password-reset-requests` with email | Keycloak `UPDATE_PASSWORD` email action is requested |
| AUTH-007 Password reset | `POST /api/v1/auth/password-resets` | Compatibility response explains that Keycloak completes the reset link |
| AUTH-008 Protected profile | `GET /api/v1/users/me` with `Authorization: Bearer <keycloak-access-token>` | HTTP 200 for valid Keycloak token; HTTP 401 without/invalid token |
| AUTH-009 Profile update | `PATCH /api/v1/users/me` with bearer token and names | HTTP 200; only authenticated user's profile changes |
| AUTH-010 Health/routing | `GET /api/health` | HTTP 200 through gateway; Swagger loads |

Example registration request:

```json
{
  "email": "testuser@example.com",
  "password": "<test-password>",
  "firstName": "Test",
  "lastName": "User"
}
```

Example protected profile request:

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/identity/api/v1/users/me" `
  -Headers $authHeaders `
  -Method Get
```

If this returns HTTP 401, confirm that `$authHeaders` was created from a
currently valid access token issued by the `workora` Keycloak realm.

The identity service validates Keycloak-issued JWTs using the configured issuer
and JWK set. The compatibility auth endpoints delegate credential and action
flows to Keycloak.

## Identity implementation status

The current Identity Service implements Keycloak-backed registration,
login/refresh/logout, verification and password-reset action requests, local
profile synchronization, profile read/update, BCrypt local password storage,
and Keycloak JWT validation. Keycloak's token, revocation, discovery, and JWKS
endpoints are used directly; the Identity Service does not reimplement those
OAuth endpoints.

Google federation, RabbitMQ identity events, complete profile attributes
(gender, date of birth, avatar, bio, and experience), account
disable/delete workflows, gateway rate limiting, refresh-token family
revocation, and production TLS/observability remain roadmap items. They are
not represented as implemented features.

The databases are exposed on host ports `5433` through `5440`; inside the
Compose network each database listens on its normal PostgreSQL port `5432`.
Each Spring Boot service is configured to use its matching database by
default. Start a service after the databases are healthy, for example:

```bash
mvn spring-boot:run -pl identity-service
```

The datasource settings support `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`
environment variables for deployment-specific connection values.

Copy `.env.example` to `.env` before starting Compose and replace every
placeholder. The `.env` file supplies database, RabbitMQ, MinIO, Keycloak,
JWT, SMTP, and internal-service credentials and is ignored by Git. Never
commit it or paste its values into logs or issue reports.

### Keycloak email configuration

Keycloak sends verification and password-reset action emails through its realm
SMTP configuration. Configure SMTP in the Keycloak administration console and
provide its credentials as deployment secrets. Do not add a duplicate Workora
OTP flow; reserve OTP for a future, separately documented MFA or high-risk
action requirement.

```powershell
docker compose logs -f keycloak
```

Do not commit SMTP credentials. Email delivery is delegated to Keycloak.

## Swagger UI

Open Swagger through the gateway using this single format:

| Service | Swagger URL |
| --- | --- |
| API Gateway | http://localhost:8080/gateway/swagger-ui.html |
| Identity | http://localhost:8080/identify/swagger-ui.html |
| Organization | http://localhost:8080/organization/swagger-ui.html |
| Project | http://localhost:8080/project/swagger-ui.html |
| Work | http://localhost:8080/work/swagger-ui.html |
| Bug | http://localhost:8080/bug/swagger-ui.html |
| Recruitment | http://localhost:8080/recruitment/swagger-ui.html |
| Notification | http://localhost:8080/notification/swagger-ui.html |
| Payment | http://localhost:8080/payment/swagger-ui.html |

Use the gateway URLs above as the single Swagger access format. The individual
service ports are for service traffic and are not the documented Swagger entry
points.

For Identity protected endpoints, click the **Authorize** lock button in
Swagger, paste only the Keycloak access token, and select **Authorize**.
Swagger will automatically send `Authorization: Bearer <token>`. Do not paste
the `Bearer ` prefix if the dialog already supplies it.

## Identity authentication

The recommended Keycloak authentication flow is:

1. Start Keycloak with `docker compose up -d keycloak`.
2. Create or manage users in the `workora` realm.
3. Request an access token for the `workora-api` client.
4. Add the token to Swagger with **Authorize**.
5. Call protected APIs through the gateway.

Keycloak realm registration and reset-password features are enabled for local
development. Production deployments should use secure administrator
credentials, HTTPS, restricted redirect URIs, and externally managed secrets.
