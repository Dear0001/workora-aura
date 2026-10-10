# Identity Service

The Identity Service stores the local Workora profile and delegates identity
operations to Keycloak. It is the only service in this repository with
implemented business endpoints; the other domain services currently expose
health checks only.

## Implemented responsibilities

- Keycloak-backed registration, login, refresh, and logout.
- Keycloak verification-email links and password-reset action links.
- Short-lived, opaque registration verification sessions for frontend polling.
- Local `users` profile synchronization with UUID IDs.
- Current-user profile read and first/last-name update.
- BCrypt password hashing for the local profile record.
- JWT resource-server validation using the configured Keycloak issuer/JWK set.
- Request validation, global exception handling, and health/Actuator endpoints.

## Implemented endpoints

Paths are shown relative to the direct service base URL
`http://localhost:8101`. Through the gateway, prefix them with
`http://localhost:8080/api/identity`.

| Method | Direct path | Authentication | Behavior |
| --- | --- | --- | --- |
| GET | `/api/health` | Public | Returns service status and description |
| POST | `/api/v1/auth/register` | Public | Creates a Keycloak user and local profile |
| GET | `/api/v1/auth/registration-verification-sessions/{token}` | Public | Polls whether the registration email was verified |
| POST | `/api/v1/auth/login` | Public | Returns Keycloak access/refresh tokens and profile data |
| POST | `/api/v1/auth/refresh` | Public | Exchanges a Keycloak refresh token |
| POST | `/api/v1/auth/logout` | Public | Revokes the supplied Keycloak refresh token |
| POST | `/api/v1/auth/password-reset-requests` | Public | Starts a Keycloak password-reset email action |
| POST | `/api/v1/auth/verification-email-requests` | Public | Resends a Keycloak verification email with a generic response |
| POST | `/api/v1/auth/password-resets` | Public | Explains that reset is completed by Keycloak email link |
| GET | `/api/v1/users/me` | Bearer token | Returns the authenticated local profile |
| PATCH | `/api/v1/users/me` | Bearer token | Updates first and last names |

The gateway strips `/api/identity` before forwarding. For example:

```text
Client:  POST /api/identity/api/v1/auth/login
Service: POST /api/v1/auth/login
```

## Not implemented yet

Google/OIDC federation, an OAuth façade, user administration and disable/delete
lifecycle, payment profiles, role/scope governance, refresh-token family
revocation owned by Workora, rate limiting, correlation IDs, and audit/event
publishing remain planned features. The target contracts are documented in
[`API-ENDPOINTS.md`](API-ENDPOINTS.md).

## Running locally

Start Keycloak and the identity database from the repository root:

```bash
docker compose up -d keycloak postgres
```

Keycloak is available at `http://localhost:8180`, using realm `workora` and
client `workora-api`. The local administrator is `admin` / `admin`; change
these development defaults before sharing the environment.

Start the service with Java 17:

```bash
mvn spring-boot:run -pl identity-service
```

The datasource defaults to database `identity_db` on `localhost:5433`. Set
`DB_USERNAME` and `DB_PASSWORD` to the Identity-specific credentials from the
backend root's `tools/postgres/roles.env.example`; do not use the PostgreSQL
administrator credentials for the service.

Configure Keycloak SMTP in the realm for verification and password-reset
emails. Keycloak is the single source of truth for both flows: do not add a
second Workora OTP for email verification or password reset. An OTP should be
introduced only later as a separate MFA or high-risk-action factor.

Both email-request endpoints intentionally return the same success message for
known and unknown addresses, preventing account enumeration. Password reset is
completed by the `UPDATE_PASSWORD` link issued by Keycloak; the link redirects
to the configured frontend reset-password status route after success. The
`/password-resets` compatibility endpoint is retained only for clients that
still call the old route; it rejects local OTP completion because Workora does
not issue or verify password-reset OTPs. SMTP and Keycloak action-email
configuration must be working for the link to be delivered.
Supply deployment secrets through `KEYCLOAK_*` and database environment
variables; do not commit credentials to configuration or test evidence.

## API documentation

- Gateway Swagger UI: `http://localhost:8080/gateway/swagger-ui.html`
- Identity Swagger UI: `http://localhost:8080/identify/swagger-ui.html`
- Identity OpenAPI: `http://localhost:8080/identify/v3/api-docs`
- Gateway health: `http://localhost:8080/api/identity/api/health`
- Actuator health: `http://localhost:8080/identify/actuator/health`
