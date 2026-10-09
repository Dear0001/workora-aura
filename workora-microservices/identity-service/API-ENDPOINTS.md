# Workora API Endpoint Map

This document translates the backend plan into a concrete REST endpoint blueprint. The design follows the architecture described in Workora-Backend-Development-Plan.md and the Spring Boot best-practice guidance in SKILL-BACKEND.md.

## Implementation status (verified 2026-09-13)

This is a target contract map, not an inventory of live controllers. The
currently implemented routes are limited to:

| Service | Implemented routes |
| --- | --- |
| Identity | `GET /api/health`; the eight `/api/v1/auth/*` routes; `GET` and `PATCH /api/v1/users/me` |
| API Gateway | Routing and Swagger aggregation for Identity, Organization, Project, Work, Bug, Recruitment, Notification, and Payment |
| Organization, Project, Work, Bug, Recruitment, Notification, Payment | `GET /api/health` only |

No endpoint listed in the target sections below should be treated as available
unless it appears in the service's controller source and README.

## 1. Shared conventions

- API version prefix: `/api/v1`
- Resource IDs are UUIDs/ULIDs, never sequential DB IDs
- Standard responses: `200 OK`, `201 Created`, `204 No Content`, `400 Bad Request`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found`, `409 Conflict`, `422 Unprocessable Entity`
- Use RFC 9457-style problem details for errors
- Use pagination: `?page=0&size=20&sort=createdAt,desc`
- Use `PATCH` for partial updates
- Require `Authorization: Bearer <token>` on protected resources
- Use `X-Correlation-ID` across requests and downstream calls

## 2. Gateway routing (implemented routes)

The API gateway exposes public routes and forwards to the owning microservice.

- `GET /health/live`
- `GET /health/ready`
- `GET /api/v1/companies/**` -> organization-service
- `GET /api/v1/projects/**` -> project-service
- `GET /api/v1/tasks/**` -> work-service
- `GET /api/v1/bug-reports/**` -> bug-service
- `GET /api/v1/recruitment-posts/**` -> recruitment-service
- `GET /api/v1/notifications/**` -> notification-service
- `GET /api/v1/payments/**` -> payment-service

## 3. Auth service

Primary database: `auth_db`

### OAuth and identity metadata

- `POST /oauth/token`
  - Exchange authorization code, refresh token, or client credentials
- `POST /oauth/revoke`
  - Revoke token family/session
- `GET /.well-known/openid-configuration`
- `GET /.well-known/jwks.json`

### Authn / account lifecycle

- `POST /api/v1/auth/register`
- `GET /api/v1/auth/registration-verification-sessions/{token}`
- `POST /api/v1/auth/password-reset-requests`
- `POST /api/v1/auth/verification-email-requests`
- `POST /api/v1/auth/password-resets`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/logout`
- `POST /api/v1/auth/refresh`

Registration returns a JSON `data` object containing `userId`, `email`,
`verificationSessionToken`, and `message`. The session token is an opaque
256-bit bearer secret and is returned only once; clients must not log or expose
it beyond the registration check-email page.

`GET /api/v1/auth/registration-verification-sessions/{token}` returns:

```json
{
  "success": true,
  "data": {
    "status": "PENDING",
    "expiresAt": "2026-09-24T09:00:00Z"
  }
}
```

`status` is `PENDING`, `VERIFIED`, or `EXPIRED`. The default lifetime is 15
minutes and is configurable with `REGISTRATION_VERIFICATION_SESSION_TTL`.
The endpoint is public because possession of the high-entropy session token is
the authorization for this narrowly scoped read; the token is stored only as
SHA-256 in the database. A verified session remains readable as `VERIFIED`
until its expiry.

### Sample requests and responses for identity endpoints

This service follows the shared `ApiResponse<T>` envelope:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "userId": "1c7d0b9a-6d93-4f3a-a2de-b8d7d51c5ad1"
  },
  "timestamp": "2026-09-24T10:12:00Z"
}
```

#### `POST /api/v1/auth/register`

Request:

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "alice@example.com",
  "password": "P@ssw0rd123",
  "firstName": "Alice",
  "lastName": "Nguyen"
}
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "userId": "1c7d0b9a-6d93-4f3a-a2de-b8d7d51c5ad1",
    "email": "alice@example.com",
    "verificationSessionToken": "Xq8K6m2H1Fj4NmEf7V9WnG6tI5uQ9QzQz2sA7m5vW2k0",
    "message": "Registration successful. Keycloak sent a verification email."
  },
  "timestamp": "2026-09-24T10:12:00Z"
}
```

#### `POST /api/v1/auth/login`

Request:

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "alice@example.com",
  "password": "P@ssw0rd123"
}
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": "1c7d0b9a-6d93-4f3a-a2de-b8d7d51c5ad1",
    "email": "alice@example.com",
    "firstName": "Alice",
    "lastName": "Nguyen",
    "role": "USER"
  },
  "timestamp": "2026-09-24T10:13:00Z"
}
```

#### `POST /api/v1/auth/refresh`

Request:

```http
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": "1c7d0b9a-6d93-4f3a-a2de-b8d7d51c5ad1",
    "email": "alice@example.com",
    "firstName": "Alice",
    "lastName": "Nguyen",
    "role": "USER"
  },
  "timestamp": "2026-09-24T10:14:00Z"
}
```

#### `POST /api/v1/auth/logout`

Request:

```http
POST /api/v1/auth/logout
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": "Logged out successfully",
  "timestamp": "2026-09-24T10:15:00Z"
}
```

#### `POST /api/v1/auth/password-reset-requests`

Request:

```http
POST /api/v1/auth/password-reset-requests
Content-Type: application/json

{
  "email": "alice@example.com"
}
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": "A password reset email will be sent if the account exists.",
  "timestamp": "2026-09-24T10:18:00Z"
}
```

#### `POST /api/v1/auth/verification-email-requests`

Request:

```http
POST /api/v1/auth/verification-email-requests
Content-Type: application/json

{
  "email": "alice@example.com"
}
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": "A verification email will be sent if the account exists.",
  "timestamp": "2026-09-24T10:18:30Z"
}
```

#### `POST /api/v1/auth/password-resets`

Request:

```http
POST /api/v1/auth/password-resets
Content-Type: application/json

{
  "email": "alice@example.com",
  "token": "482631",
  "newPassword": "NewP@ssw0rd456"
}
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": "Password reset successful",
  "timestamp": "2026-09-24T10:20:00Z"
}
```

> Note: In the current implementation, the password reset flow is finalized through the Keycloak email link and the reset endpoint intentionally rejects an in-app flow for the particular stateful reset form. The example above reflects the intended contract; the runtime path currently throws an explicit `AppException` if a direct reset request is attempted without the email-link flow.

#### `GET /api/v1/auth/registration-verification-sessions/{token}`

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "status": "PENDING",
    "expiresAt": "2026-09-24T10:30:00Z"
  },
  "timestamp": "2026-09-24T10:13:45Z"
}
```

### User profile and me endpoints

#### `GET /api/v1/users/me`

Request:

```http
GET /api/v1/users/me
Authorization: Bearer <access-token>
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "id": "1c7d0b9a-6d93-4f3a-a2de-b8d7d51c5ad1",
    "email": "alice@example.com",
    "firstName": "Alice",
    "lastName": "Nguyen",
    "gender": "FEMALE",
    "dateOfBirth": "1994-05-21",
    "avatarUrl": "https://cdn.example.com/users/alice.png",
    "bio": "Product engineer focused on developer experience.",
    "experience": "5 years in full-stack development",
    "role": "USER",
    "emailVerified": true,
    "accountStatus": "ACTIVE"
  },
  "timestamp": "2026-09-24T10:16:00Z"
}
```

#### `PATCH /api/v1/users/me`

Request:

```http
PATCH /api/v1/users/me
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "firstName": "Alicia",
  "lastName": "Nguyen",
  "gender": "FEMALE",
  "dateOfBirth": "1994-05-21",
  "avatarUrl": "https://cdn.example.com/users/alice.png",
  "bio": "Product engineer focused on platform reliability.",
  "experience": "6 years in backend and developer tooling"
}
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "id": "1c7d0b9a-6d93-4f3a-a2de-b8d7d51c5ad1",
    "email": "alice@example.com",
    "firstName": "Alicia",
    "lastName": "Nguyen",
    "gender": "FEMALE",
    "dateOfBirth": "1994-05-21",
    "avatarUrl": "https://cdn.example.com/users/alice.png",
    "bio": "Product engineer focused on platform reliability.",
    "experience": "6 years in backend and developer tooling",
    "role": "USER",
    "emailVerified": true,
    "accountStatus": "ACTIVE"
  },
  "timestamp": "2026-09-24T10:17:00Z"
}
```

#### `GET /api/v1/users/{userId}`

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "id": "1c7d0b9a-6d93-4f3a-a2de-b8d7d51c5ad1",
    "email": "alice@example.com",
    "firstName": "Alice",
    "lastName": "Nguyen",
    "role": "USER",
    "accountStatus": "ACTIVE"
  },
  "timestamp": "2026-09-24T10:18:00Z"
}
```

#### `PATCH /api/v1/users/{userId}` (admin only)

Request:

```http
PATCH /api/v1/users/1c7d0b9a-6d93-4f3a-a2de-b8d7d51c5ad1
Authorization: Bearer <admin-access-token>
Content-Type: application/json

{
  "role": "ADMIN",
  "accountStatus": "ACTIVE"
}
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "id": "1c7d0b9a-6d93-4f3a-a2de-b8d7d51c5ad1",
    "email": "alice@example.com",
    "role": "ADMIN",
    "accountStatus": "ACTIVE"
  },
  "timestamp": "2026-09-24T10:19:00Z"
}
```

#### `DELETE /api/v1/users/{userId}`

Response:

```json
{
  "success": true,
  "message": "User deactivated successfully",
  "data": null,
  "timestamp": "2026-09-24T10:20:00Z"
}
```

### Payment profile endpoints

- `GET /api/v1/users/me`
- `PATCH /api/v1/users/me`
- `GET /api/v1/users/{userId}`
- `PATCH /api/v1/users/{userId}` (admin only)
- `DELETE /api/v1/users/{userId}` (soft delete / disable)

### Payment profile endpoints

- `GET /api/v1/users/me/payment-profiles/bakong`
- `POST /api/v1/users/me/payment-profiles/bakong`
- `PATCH /api/v1/users/me/payment-profiles/bakong`
- `DELETE /api/v1/users/me/payment-profiles/bakong`
- `POST /api/v1/users/me/payment-profiles/bakong/qr-upload`

#### `POST /api/v1/users/me/payment-profiles/bakong`

Request:

```http
POST /api/v1/users/me/payment-profiles/bakong
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "walletName": "Alice Nguyen",
  "phoneNumber": "+85512345678",
  "isPrimary": true
}
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "id": "bfc0679a-8d10-43ba-bf47-0660b15f35b0",
    "walletName": "Alice Nguyen",
    "phoneNumber": "+85512345678",
    "isPrimary": true,
    "status": "ACTIVE"
  },
  "timestamp": "2026-09-24T10:21:00Z"
}
```

### OAuth / role scopes

- `openid`
- `profile`
- `email`
- `company:read`
- `company:write`
- `project:read`
- `project:write`
- `task:read`
- `task:write`
- `bug:read`
- `bug:write`
- `recruitment:read`
- `recruitment:write`
- `notification:read`
- `notification:write`
- `payment:read`
- `payment:write`

### Representative examples for downstream service endpoint contracts

These services follow the same `ApiResponse<T>` envelope and payload style. The samples below are intentionally illustrative but should be treated as the contract shape for the corresponding route groups.

#### Organization service: `POST /api/v1/companies`

Request:

```http
POST /api/v1/companies
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "name": "Workora Labs",
  "industry": "Software",
  "country": "KH",
  "website": "https://workora.app"
}
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "id": "d4e6d5ea-d9e5-4d0a-b463-3c3b1ec38bc5",
    "name": "Workora Labs",
    "industry": "Software",
    "country": "KH",
    "website": "https://workora.app",
    "status": "ACTIVE"
  },
  "timestamp": "2026-09-24T10:22:00Z"
}
```

#### Project service: `POST /api/v1/companies/{companyId}/projects`

Request:

```http
POST /api/v1/companies/d4e6d5ea-d9e5-4d0a-b463-3c3b1ec38bc5/projects
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "name": "Mobile app refresh",
  "description": "Redesign the mobile experience for the dashboard and onboarding flow.",
  "visibility": "INTERNAL"
}
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "id": "7354f4c2-32ce-42bf-a9de-d9fd0be85870",
    "companyId": "d4e6d5ea-d9e5-4d0a-b463-3c3b1ec38bc5",
    "name": "Mobile app refresh",
    "description": "Redesign the mobile experience for the dashboard and onboarding flow.",
    "visibility": "INTERNAL",
    "status": "ACTIVE"
  },
  "timestamp": "2026-09-24T10:23:00Z"
}
```

#### Work service: `POST /api/v1/phases/{phaseId}/tasks`

Request:

```http
POST /api/v1/phases/3ca8d3c2-0ffd-4d2d-a0d9-bdd8c9f1010f/tasks
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "title": "Refine onboarding checklist",
  "description": "Review the onboarding checklist and remove duplicate steps.",
  "priority": "HIGH",
  "assigneeIds": ["1c7d0b9a-6d93-4f3a-a2de-b8d7d51c5ad1"]
}
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "id": "f74f8110-5592-4d99-ac68-51c5fe31e2d3",
    "phaseId": "3ca8d3c2-0ffd-4d2d-a0d9-bdd8c9f1010f",
    "title": "Refine onboarding checklist",
    "description": "Review the onboarding checklist and remove duplicate steps.",
    "priority": "HIGH",
    "status": "TODO",
    "assigneeIds": ["1c7d0b9a-6d93-4f3a-a2de-b8d7d51c5ad1"]
  },
  "timestamp": "2026-09-24T10:24:00Z"
}
```

#### Bug service: `POST /api/v1/tasks/{taskId}/bug-reports`

Request:

```http
POST /api/v1/tasks/f74f8110-5592-4d99-ac68-51c5fe31e2d3/bug-reports
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "title": "Login button is missing on mobile",
  "description": "On the Android build, the sign-in button is not visible on the first screen.",
  "severity": "HIGH"
}
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "id": "8f7251f0-597d-4b0b-8bd3-89b610b42991",
    "taskId": "f74f8110-5592-4d99-ac68-51c5fe31e2d3",
    "title": "Login button is missing on mobile",
    "status": "OPEN",
    "severity": "HIGH"
  },
  "timestamp": "2026-09-24T10:25:00Z"
}
```

#### Recruitment service: `POST /api/v1/recruitment-posts`

Request:

```http
POST /api/v1/recruitment-posts
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "title": "Senior Backend Engineer",
  "location": "Phnom Penh",
  "employmentType": "FULL_TIME",
  "description": "Build and scale the Workora identity and account platform."
}
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "id": "7c5d45ce-9ac7-4618-bb31-ed0dd93d2d04",
    "title": "Senior Backend Engineer",
    "location": "Phnom Penh",
    "employmentType": "FULL_TIME",
    "status": "OPEN"
  },
  "timestamp": "2026-09-24T10:26:00Z"
}
```

#### Notification service: `GET /api/v1/notifications`

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "items": [
      {
        "id": "5f66a1e1-2d10-4a06-832c-e379bc542c41",
        "type": "TASK_ASSIGNED",
        "message": "You were assigned to task: Refine onboarding checklist",
        "read": false,
        "createdAt": "2026-09-24T10:26:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "total": 1
  },
  "timestamp": "2026-09-24T10:27:00Z"
}
```

#### Payment service: `POST /api/v1/bug-fix-jobs/{jobId}/fee-approval`

Request:

```http
POST /api/v1/bug-fix-jobs/1f82e7c3-8700-44d0-ac15-a9f5f6c58b32/fee-approval
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "approvedAmount": 150.00,
  "currency": "USD",
  "note": "Approved after QA review"
}
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "id": "8d6af901-f9d8-4860-afce-733c72132b8b",
    "jobId": "1f82e7c3-8700-44d0-ac15-a9f5f6c58b32",
    "approvedAmount": 150.00,
    "currency": "USD",
    "status": "APPROVED"
  },
  "timestamp": "2026-09-24T10:28:00Z"
}
```

#### Feed and query services: `GET /api/v1/feed`

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "items": [
      {
        "type": "PROJECT",
        "id": "7354f4c2-32ce-42bf-a9de-d9fd0be85870",
        "title": "Mobile app refresh",
        "updatedAt": "2026-09-24T10:28:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "total": 1
  },
  "timestamp": "2026-09-24T10:29:00Z"
}
```

## 4. User service

Primary database: `user_db`

- `GET /api/v1/users`
  - list users with filtering by company/project role or status
- `GET /api/v1/users/{userId}/profile`
- `PATCH /api/v1/users/{userId}/profile`
- `GET /api/v1/users/{userId}/experience`
- `PATCH /api/v1/users/{userId}/experience`
- `GET /api/v1/users/{userId}/verification-status`
- `POST /api/v1/users/{userId}/disable`
- `POST /api/v1/users/{userId}/enable`
- `GET /api/v1/users/{userId}/activity`

### Sample request/response examples

#### `GET /api/v1/users?page=0&size=20&role=PROJECT_MANAGER`

Request:

```http
GET /api/v1/users?page=0&size=20&role=PROJECT_MANAGER
Authorization: Bearer <access-token>
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "items": [
      {
        "id": "1c7d0b9a-6d93-4f3a-a2de-b8d7d51c5ad1",
        "email": "alice@example.com",
        "firstName": "Alice",
        "lastName": "Nguyen",
        "role": "PROJECT_MANAGER",
        "accountStatus": "ACTIVE"
      }
    ],
    "page": 0,
    "size": 20,
    "total": 1
  },
  "timestamp": "2026-09-24T10:30:00Z"
}
```

#### `GET /api/v1/users/{userId}/profile`

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "id": "1c7d0b9a-6d93-4f3a-a2de-b8d7d51c5ad1",
    "email": "alice@example.com",
    "firstName": "Alice",
    "lastName": "Nguyen",
    "gender": "FEMALE",
    "bio": "Platform engineer",
    "experience": "5 years in product engineering",
    "verificationStatus": "VERIFIED"
  },
  "timestamp": "2026-09-24T10:30:10Z"
}
```

#### `PATCH /api/v1/users/{userId}/profile`

Request:

```http
PATCH /api/v1/users/1c7d0b9a-6d93-4f3a-a2de-b8d7d51c5ad1/profile
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "bio": "Platform engineer focused on API reliability.",
  "experience": "6 years in backend architecture"
}
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "id": "1c7d0b9a-6d93-4f3a-a2de-b8d7d51c5ad1",
    "bio": "Platform engineer focused on API reliability.",
    "experience": "6 years in backend architecture",
    "verificationStatus": "VERIFIED"
  },
  "timestamp": "2026-09-24T10:30:20Z"
}
```

#### `POST /api/v1/users/{userId}/disable`

Request:

```http
POST /api/v1/users/1c7d0b9a-6d93-4f3a-a2de-b8d7d51c5ad1/disable
Authorization: Bearer <admin-access-token>
Content-Type: application/json

{
  "reason": "Inactivity"
}
```

Response:

```json
{
  "success": true,
  "message": "User disabled successfully",
  "data": {
    "userId": "1c7d0b9a-6d93-4f3a-a2de-b8d7d51c5ad1",
    "accountStatus": "DISABLED"
  },
  "timestamp": "2026-09-24T10:30:45Z"
}
```

## 5. Organization service

Primary database: `organization_db`

### Company endpoints

- `POST /api/v1/companies`
- `GET /api/v1/companies/{companyId}`
- `PATCH /api/v1/companies/{companyId}`
- `DELETE /api/v1/companies/{companyId}` (archive / soft delete)
- `GET /api/v1/companies`
- `GET /api/v1/companies/{companyId}/profile`
- `GET /api/v1/companies/{companyId}/posts`
- `GET /api/v1/companies/{companyId}/feedback`
- `GET /api/v1/companies/{companyId}/ratings`

### Department endpoints

- `POST /api/v1/companies/{companyId}/departments`
- `GET /api/v1/companies/{companyId}/departments`
- `GET /api/v1/departments/{departmentId}`
- `PATCH /api/v1/departments/{departmentId}`
- `DELETE /api/v1/departments/{departmentId}`
- `GET /api/v1/departments/{departmentId}/members`
- `POST /api/v1/departments/{departmentId}/members/{userId}`
- `DELETE /api/v1/departments/{departmentId}/members/{userId}`

### Company membership / role endpoints

- `POST /api/v1/companies/{companyId}/members/invitations`
- `GET /api/v1/companies/{companyId}/invitations`
- `POST /api/v1/companies/{companyId}/invitations/{invitationId}/accept`
- `POST /api/v1/companies/{companyId}/members/{userId}/roles`
- `PATCH /api/v1/companies/{companyId}/members/{userId}/roles`
- `DELETE /api/v1/companies/{companyId}/members/{userId}`
- `GET /api/v1/companies/{companyId}/members`

### Rating endpoints

- `POST /api/v1/companies/{companyId}/ratings`
- `GET /api/v1/companies/{companyId}/ratings`
- `GET /api/v1/users/{userId}/ratings`
- `PATCH /api/v1/ratings/{ratingId}`
- `POST /api/v1/ratings/{ratingId}/response`

### Sample request/response examples

#### `POST /api/v1/companies`

Request:

```http
POST /api/v1/companies
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "name": "Workora Labs",
  "industry": "Software",
  "country": "KH",
  "website": "https://workora.app"
}
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "id": "d4e6d5ea-d9e5-4d0a-b463-3c3b1ec38bc5",
    "name": "Workora Labs",
    "industry": "Software",
    "country": "KH",
    "website": "https://workora.app",
    "status": "ACTIVE"
  },
  "timestamp": "2026-09-24T10:32:00Z"
}
```

#### `POST /api/v1/companies/{companyId}/departments`

Request:

```http
POST /api/v1/companies/d4e6d5ea-d9e5-4d0a-b463-3c3b1ec38bc5/departments
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "name": "Engineering",
  "description": "Product and platform engineering"
}
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "id": "11f8c7d4-8752-4a77-8d23-f698d87b65b7",
    "companyId": "d4e6d5ea-d9e5-4d0a-b463-3c3b1ec38bc5",
    "name": "Engineering",
    "description": "Product and platform engineering",
    "memberCount": 0
  },
  "timestamp": "2026-09-24T10:32:20Z"
}
```

#### `POST /api/v1/companies/{companyId}/members/invitations`

Request:

```http
POST /api/v1/companies/d4e6d5ea-d9e5-4d0a-b463-3c3b1ec38bc5/members/invitations
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "email": "bob@example.com",
  "role": "DEVELOPER",
  "departmentId": "11f8c7d4-8752-4a77-8d23-f698d87b65b7"
}
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "id": "c7d89257-296b-41f2-b3ea-ff9af7d9a511",
    "companyId": "d4e6d5ea-d9e5-4d0a-b463-3c3b1ec38bc5",
    "email": "bob@example.com",
    "role": "DEVELOPER",
    "status": "PENDING"
  },
  "timestamp": "2026-09-24T10:32:45Z"
}
```

#### `POST /api/v1/companies/{companyId}/ratings`

Request:

```http
POST /api/v1/companies/d4e6d5ea-d9e5-4d0a-b463-3c3b1ec38bc5/ratings
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "score": 5,
  "review": "Excellent communication and strong delivery quality."
}
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "id": "8e99ba35-019f-42d8-adb8-f2d5e36c8f40",
    "companyId": "d4e6d5ea-d9e5-4d0a-b463-3c3b1ec38bc5",
    "score": 5,
    "review": "Excellent communication and strong delivery quality.",
    "createdAt": "2026-09-24T10:33:00Z"
  },
  "timestamp": "2026-09-24T10:33:00Z"
}
```

## 6. Project service

Primary database: `project_db`

### Project endpoints

- `POST /api/v1/companies/{companyId}/projects`
- `GET /api/v1/companies/{companyId}/projects`
- `GET /api/v1/departments/{departmentId}/projects`
- `GET /api/v1/projects/{projectId}`
- `PATCH /api/v1/projects/{projectId}`
- `DELETE /api/v1/projects/{projectId}`
- `POST /api/v1/projects/{projectId}/archive`
- `GET /api/v1/projects/{projectId}/members`
- `POST /api/v1/projects/{projectId}/managers/{userId}`

### Phase endpoints

- `POST /api/v1/projects/{projectId}/phases`
- `GET /api/v1/projects/{projectId}/phases`
- `GET /api/v1/phases/{phaseId}`
- `PATCH /api/v1/phases/{phaseId}`
- `DELETE /api/v1/phases/{phaseId}`
- `POST /api/v1/phases/{phaseId}/publish`
- `POST /api/v1/phases/{phaseId}/hide`
- `POST /api/v1/phases/{phaseId}/close`
- `GET /api/v1/public/phases`
- `GET /api/v1/public/phases/{phaseId}`
- `GET /api/v1/phases/{phaseId}/members`
- `POST /api/v1/phases/{phaseId}/members/{userId}`
- `DELETE /api/v1/phases/{phaseId}/members/{userId}`

### Bug-Fix Job endpoints

- `POST /api/v1/phases/{phaseId}/bug-fix-jobs`
- `GET /api/v1/phases/{phaseId}/bug-fix-jobs`
- `GET /api/v1/bug-fix-jobs/{jobId}`
- `PATCH /api/v1/bug-fix-jobs/{jobId}`
- `POST /api/v1/bug-fix-jobs/{jobId}/publish`
- `POST /api/v1/bug-fix-jobs/{jobId}/close`
- `POST /api/v1/bug-fix-jobs/{jobId}/fund`

### Sample request/response examples

#### `POST /api/v1/companies/{companyId}/projects`

Request:

```http
POST /api/v1/companies/d4e6d5ea-d9e5-4d0a-b463-3c3b1ec38bc5/projects
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "name": "Mobile app refresh",
  "description": "Redesign the mobile experience for the dashboard and onboarding flow.",
  "visibility": "INTERNAL"
}
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "id": "7354f4c2-32ce-42bf-a9de-d9fd0be85870",
    "companyId": "d4e6d5ea-d9e5-4d0a-b463-3c3b1ec38bc5",
    "name": "Mobile app refresh",
    "description": "Redesign the mobile experience for the dashboard and onboarding flow.",
    "status": "ACTIVE",
    "visibility": "INTERNAL"
  },
  "timestamp": "2026-09-24T10:34:00Z"
}
```

#### `POST /api/v1/projects/{projectId}/phases`

Request:

```http
POST /api/v1/projects/7354f4c2-32ce-42bf-a9de-d9fd0be85870/phases
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "name": "Design phase",
  "startDate": "2026-09-25",
  "endDate": "2026-10-15"
}
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "id": "3ca8d3c2-0ffd-4d2d-a0d9-bdd8c9f1010f",
    "projectId": "7354f4c2-32ce-42bf-a9de-d9fd0be85870",
    "name": "Design phase",
    "status": "ACTIVE",
    "startDate": "2026-09-25",
    "endDate": "2026-10-15"
  },
  "timestamp": "2026-09-24T10:34:30Z"
}
```

#### `POST /api/v1/phases/{phaseId}/bug-fix-jobs`

Request:

```http
POST /api/v1/phases/3ca8d3c2-0ffd-4d2d-a0d9-bdd8c9f1010f/bug-fix-jobs
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "title": "Fix onboarding crash",
  "budget": 200,
  "currency": "USD"
}
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "id": "1f82e7c3-8700-44d0-ac15-a9f5f6c58b32",
    "phaseId": "3ca8d3c2-0ffd-4d2d-a0d9-bdd8c9f1010f",
    "title": "Fix onboarding crash",
    "budget": 200,
    "currency": "USD",
    "status": "OPEN"
  },
  "timestamp": "2026-09-24T10:35:00Z"
}
```

## 7. Work service

Primary database: `task_db`

### Task CRUD and assignment

- `POST /api/v1/phases/{phaseId}/tasks`
- `GET /api/v1/phases/{phaseId}/tasks`
- `GET /api/v1/tasks/{taskId}`
- `PATCH /api/v1/tasks/{taskId}`
- `DELETE /api/v1/tasks/{taskId}`
- `GET /api/v1/tasks/{taskId}/assignees`
- `POST /api/v1/tasks/{taskId}/assignees`
- `DELETE /api/v1/tasks/{taskId}/assignees/{userId}`
- `PATCH /api/v1/tasks/{taskId}/status`

### Task submissions and attachments

- `POST /api/v1/tasks/{taskId}/submissions`
- `GET /api/v1/tasks/{taskId}/submissions`
- `GET /api/v1/tasks/{taskId}/submissions/{submissionId}`
- `GET /api/v1/tasks/{taskId}/attachments`
- `POST /api/v1/tasks/{taskId}/attachments`
- `DELETE /api/v1/tasks/{taskId}/attachments/{attachmentId}`

### Workspace and query views

- `GET /api/v1/workspace`
- `GET /api/v1/dashboard`
- `GET /api/v1/history`

### Sample request/response examples

#### `POST /api/v1/phases/{phaseId}/tasks`

Request:

```http
POST /api/v1/phases/3ca8d3c2-0ffd-4d2d-a0d9-bdd8c9f1010f/tasks
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "title": "Refine onboarding checklist",
  "description": "Review the onboarding checklist and remove duplicate steps.",
  "priority": "HIGH",
  "assigneeIds": ["1c7d0b9a-6d93-4f3a-a2de-b8d7d51c5ad1"]
}
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "id": "f74f8110-5592-4d99-ac68-51c5fe31e2d3",
    "phaseId": "3ca8d3c2-0ffd-4d2d-a0d9-bdd8c9f1010f",
    "title": "Refine onboarding checklist",
    "description": "Review the onboarding checklist and remove duplicate steps.",
    "priority": "HIGH",
    "status": "TODO",
    "assigneeIds": ["1c7d0b9a-6d93-4f3a-a2de-b8d7d51c5ad1"]
  },
  "timestamp": "2026-09-24T10:36:00Z"
}
```

#### `PATCH /api/v1/tasks/{taskId}/status`

Request:

```http
PATCH /api/v1/tasks/f74f8110-5592-4d99-ac68-51c5fe31e2d3/status
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "status": "IN_PROGRESS"
}
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "id": "f74f8110-5592-4d99-ac68-51c5fe31e2d3",
    "status": "IN_PROGRESS",
    "updatedAt": "2026-09-24T10:36:20Z"
  },
  "timestamp": "2026-09-24T10:36:20Z"
}
```

#### `POST /api/v1/tasks/{taskId}/submissions`

Request:

```http
POST /api/v1/tasks/f74f8110-5592-4d99-ac68-51c5fe31e2d3/submissions
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "summary": "Checklist cleaned up and duplicate steps removed.",
  "repositoryUrl": "https://github.com/workora/app/pull/245"
}
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "id": "6dbb0590-0f4f-494a-a9d5-ef3c18d48631",
    "taskId": "f74f8110-5592-4d99-ac68-51c5fe31e2d3",
    "summary": "Checklist cleaned up and duplicate steps removed.",
    "repositoryUrl": "https://github.com/workora/app/pull/245",
    "submittedAt": "2026-09-24T10:37:00Z"
  },
  "timestamp": "2026-09-24T10:37:00Z"
}
```

#### `GET /api/v1/workspace`

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "projects": [
      {
        "id": "7354f4c2-32ce-42bf-a9de-d9fd0be85870",
        "name": "Mobile app refresh",
        "status": "ACTIVE"
      }
    ],
    "tasks": [
      {
        "id": "f74f8110-5592-4d99-ac68-51c5fe31e2d3",
        "title": "Refine onboarding checklist",
        "status": "IN_PROGRESS"
      }
    ]
  },
  "timestamp": "2026-09-24T10:37:15Z"
}
```

## 8. Bug service

Primary database: `bug_db`

### Bug report lifecycle

- `POST /api/v1/phases/{phaseId}/bug-reports`
- `POST /api/v1/tasks/{taskId}/bug-reports`
- `GET /api/v1/bug-reports/{reportId}`
- `GET /api/v1/bug-reports`
- `PATCH /api/v1/bug-reports/{reportId}`
- `POST /api/v1/bug-reports/{reportId}/triage`
- `POST /api/v1/bug-reports/{reportId}/accept`
- `POST /api/v1/bug-reports/{reportId}/reject`
- `POST /api/v1/bug-reports/{reportId}/resolve`
- `POST /api/v1/bug-reports/{reportId}/duplicate`
- `POST /api/v1/bug-reports/{reportId}/close`

### Evidence and attachments

- `POST /api/v1/bug-reports/{reportId}/evidence`
- `GET /api/v1/bug-reports/{reportId}/evidence`
- `DELETE /api/v1/bug-reports/{reportId}/evidence/{evidenceId}`

### Sample request/response examples

#### `POST /api/v1/tasks/{taskId}/bug-reports`

Request:

```http
POST /api/v1/tasks/f74f8110-5592-4d99-ac68-51c5fe31e2d3/bug-reports
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "title": "Login button missing on mobile",
  "description": "On the Android build, the sign-in button is missing on the first screen.",
  "severity": "HIGH"
}
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "id": "8f7251f0-597d-4b0b-8bd3-89b610b42991",
    "taskId": "f74f8110-5592-4d99-ac68-51c5fe31e2d3",
    "title": "Login button missing on mobile",
    "status": "OPEN",
    "severity": "HIGH"
  },
  "timestamp": "2026-09-24T10:38:00Z"
}
```

#### `POST /api/v1/bug-reports/{reportId}/triage`

Request:

```http
POST /api/v1/bug-reports/8f7251f0-597d-4b0b-8bd3-89b610b42991/triage
Authorization: Bearer <moderator-access-token>
Content-Type: application/json

{
  "priority": "P1",
  "assigneeId": "1c7d0b9a-6d93-4f3a-a2de-b8d7d51c5ad1",
  "notes": "Reproduced on Android 14; likely regression in auth screen."
}
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "id": "8f7251f0-597d-4b0b-8bd3-89b610b42991",
    "status": "TRIAGED",
    "priority": "P1",
    "assigneeId": "1c7d0b9a-6d93-4f3a-a2de-b8d7d51c5ad1"
  },
  "timestamp": "2026-09-24T10:38:30Z"
}
```

#### `POST /api/v1/bug-reports/{reportId}/evidence`

Request:

```http
POST /api/v1/bug-reports/8f7251f0-597d-4b0b-8bd3-89b610b42991/evidence
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "type": "SCREENSHOT",
  "url": "https://cdn.example.com/evidence/bug-123.png",
  "description": "Android screenshot showing missing login CTA"
}
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "id": "2f96f04d-dbc8-4f90-8c0a-74d5f0f25d60",
    "bugReportId": "8f7251f0-597d-4b0b-8bd3-89b610b42991",
    "type": "SCREENSHOT",
    "url": "https://cdn.example.com/evidence/bug-123.png",
    "description": "Android screenshot showing missing login CTA"
  },
  "timestamp": "2026-09-24T10:39:00Z"
}
```

## 9. Recruitment service

Primary database: `recruitment_db`

### Recruitment posts

- `POST /api/v1/recruitment-posts`
- `GET /api/v1/recruitment-posts`
- `GET /api/v1/recruitment-posts/{postId}`
- `PATCH /api/v1/recruitment-posts/{postId}`
- `DELETE /api/v1/recruitment-posts/{postId}`
- `POST /api/v1/recruitment-posts/{postId}/close`

### Applications

- `POST /api/v1/recruitment-posts/{postId}/applications`
- `GET /api/v1/recruitment-posts/{postId}/applications`
- `GET /api/v1/applications/{applicationId}`
- `PATCH /api/v1/applications/{applicationId}`
- `POST /api/v1/applications/{applicationId}/manager-review`
- `POST /api/v1/applications/{applicationId}/owner-approval`
- `POST /api/v1/applications/{applicationId}/withdraw`

### Sample request/response examples

#### `POST /api/v1/recruitment-posts`

Request:

```http
POST /api/v1/recruitment-posts
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "title": "Senior Backend Engineer",
  "location": "Phnom Penh",
  "employmentType": "FULL_TIME",
  "description": "Build and scale the Workora identity and account platform."
}
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "id": "7c5d45ce-9ac7-4618-bb31-ed0dd93d2d04",
    "title": "Senior Backend Engineer",
    "location": "Phnom Penh",
    "employmentType": "FULL_TIME",
    "status": "OPEN"
  },
  "timestamp": "2026-09-24T10:40:00Z"
}
```

#### `POST /api/v1/recruitment-posts/{postId}/applications`

Request:

```http
POST /api/v1/recruitment-posts/7c5d45ce-9ac7-4618-bb31-ed0dd93d2d04/applications
Authorization: Bearer <candidate-access-token>
Content-Type: application/json

{
  "coverLetter": "I am excited to contribute to Workora's platform and scale operations.",
  "portfolioUrl": "https://portfolio.example.com/alice"
}
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "id": "fe9fd6a7-28ec-43d8-a75f-11e068032c1a",
    "postId": "7c5d45ce-9ac7-4618-bb31-ed0dd93d2d04",
    "status": "APPLIED",
    "coverLetter": "I am excited to contribute to Workora's platform and scale operations.",
    "portfolioUrl": "https://portfolio.example.com/alice"
  },
  "timestamp": "2026-09-24T10:40:20Z"
}
```

#### `POST /api/v1/applications/{applicationId}/manager-review`

Request:

```http
POST /api/v1/applications/fe9fd6a7-28ec-43d8-a75f-11e068032c1a/manager-review
Authorization: Bearer <manager-access-token>
Content-Type: application/json

{
  "decision": "SHORTLISTED",
  "notes": "Strong technical profile and relevant seniority."
}
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "id": "fe9fd6a7-28ec-43d8-a75f-11e068032c1a",
    "status": "SHORTLISTED",
    "decision": "SHORTLISTED",
    "notes": "Strong technical profile and relevant seniority."
  },
  "timestamp": "2026-09-24T10:40:55Z"
}
```

## 10. Notification service

Primary database: `notification_db`

- `GET /api/v1/notifications`
- `GET /api/v1/notifications/{notificationId}`
- `PATCH /api/v1/notifications/{notificationId}/read`
- `PATCH /api/v1/notifications/{notificationId}/archive`
- `DELETE /api/v1/notifications/{notificationId}`
- `GET /api/v1/notifications/preferences`
- `PATCH /api/v1/notifications/preferences`
- `POST /api/v1/notifications/test-email`

### Sample request/response examples

#### `GET /api/v1/notifications`

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "items": [
      {
        "id": "5f66a1e1-2d10-4a06-832c-e379bc542c41",
        "type": "TASK_ASSIGNED",
        "message": "You were assigned to task: Refine onboarding checklist",
        "read": false,
        "createdAt": "2026-09-24T10:26:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "total": 1
  },
  "timestamp": "2026-09-24T10:41:00Z"
}
```

#### `PATCH /api/v1/notifications/{notificationId}/read`

Request:

```http
PATCH /api/v1/notifications/5f66a1e1-2d10-4a06-832c-e379bc542c41/read
Authorization: Bearer <access-token>
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "id": "5f66a1e1-2d10-4a06-832c-e379bc542c41",
    "read": true,
    "readAt": "2026-09-24T10:41:10Z"
  },
  "timestamp": "2026-09-24T10:41:10Z"
}
```

#### `GET /api/v1/notifications/preferences`

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "emailEnabled": true,
    "taskReminder": true,
    "marketing": false,
    "digestFrequency": "DAILY"
  },
  "timestamp": "2026-09-24T10:41:30Z"
}
```

## 11. Payment service

Primary database: `payment_db`

### Fee and payment orchestration

- `POST /api/v1/bug-fix-jobs/{jobId}/fee-approval`
- `GET /api/v1/fee-payments/{paymentId}`
- `GET /api/v1/fee-payments/{paymentId}/qr`
- `POST /api/v1/fee-payments/{paymentId}/verify`
- `POST /api/v1/fee-payments/{paymentId}/confirm-receipt`
- `POST /api/v1/fee-payments/{paymentId}/dispute`
- `GET /api/v1/disputes/{disputeId}`

### Bakong profile endpoints

- `GET /api/v1/users/me/payment-profiles/bakong`
- `POST /api/v1/users/me/payment-profiles/bakong`
- `PATCH /api/v1/users/me/payment-profiles/bakong`
- `DELETE /api/v1/users/me/payment-profiles/bakong`
- `POST /api/v1/users/me/payment-profiles/bakong/qr-upload`

### Sample request/response examples

#### `POST /api/v1/bug-fix-jobs/{jobId}/fee-approval`

Request:

```http
POST /api/v1/bug-fix-jobs/1f82e7c3-8700-44d0-ac15-a9f5f6c58b32/fee-approval
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "approvedAmount": 150.00,
  "currency": "USD",
  "note": "Approved after QA review"
}
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "id": "8d6af901-f9d8-4860-afce-733c72132b8b",
    "jobId": "1f82e7c3-8700-44d0-ac15-a9f5f6c58b32",
    "approvedAmount": 150.00,
    "currency": "USD",
    "status": "APPROVED"
  },
  "timestamp": "2026-09-24T10:42:00Z"
}
```

#### `POST /api/v1/fee-payments/{paymentId}/verify`

Request:

```http
POST /api/v1/fee-payments/8d6af901-f9d8-4860-afce-733c72132b8b/verify
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "transactionId": "TXN-20260924-0078",
  "paymentMethod": "BAKONG"
}
```

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "paymentId": "8d6af901-f9d8-4860-afce-733c72132b8b",
    "status": "VERIFIED",
    "verifiedAt": "2026-09-24T10:42:15Z"
  },
  "timestamp": "2026-09-24T10:42:15Z"
}
```

#### `GET /api/v1/users/me/payment-profiles/bakong`

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "id": "bfc0679a-8d10-43ba-bf47-0660b15f35b0",
    "walletName": "Alice Nguyen",
    "phoneNumber": "+85512345678",
    "isPrimary": true,
    "status": "ACTIVE"
  },
  "timestamp": "2026-09-24T10:42:30Z"
}
```

## 12. Feed and query services

These are read-model endpoints built from event projections.

- `GET /api/v1/feed`
- `GET /api/v1/feed/opportunities`
- `GET /api/v1/feed/companies`
- `GET /api/v1/dashboard`
- `GET /api/v1/dashboard/company/{companyId}`
- `GET /api/v1/dashboard/project/{projectId}`
- `GET /api/v1/history`
- `GET /api/v1/workspace`

### Sample request/response examples

#### `GET /api/v1/feed`

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "items": [
      {
        "type": "PROJECT",
        "id": "7354f4c2-32ce-42bf-a9de-d9fd0be85870",
        "title": "Mobile app refresh",
        "updatedAt": "2026-09-24T10:28:00Z"
      },
      {
        "type": "RECRUITMENT",
        "id": "7c5d45ce-9ac7-4618-bb31-ed0dd93d2d04",
        "title": "Senior Backend Engineer",
        "updatedAt": "2026-09-24T10:28:10Z"
      }
    ],
    "page": 0,
    "size": 20,
    "total": 2
  },
  "timestamp": "2026-09-24T10:43:00Z"
}
```

#### `GET /api/v1/dashboard/project/{projectId}`

Response:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "projectId": "7354f4c2-32ce-42bf-a9de-d9fd0be85870",
    "tasksCompleted": 18,
    "tasksOpen": 5,
    "bugsOpen": 2,
    "teamMembers": 9
  },
  "timestamp": "2026-09-24T10:43:15Z"
}
```

## 13. Recommended package layout per service

```text
service-name/
  src/main/java/com/workora/service/
    config/
    controller/
    dto/
      request/
      response/
    entity/
    exception/
    mapper/
    repository/
    service/
    client/
    event/
    util/
```

## 14. Key implementation notes

- Each service owns its own database and publishes events to RabbitMQ/Kafka.
- Cross-service access should use IDs and events, not direct DB joins.
- The API gateway validates tokens and routes requests; domain services enforce resource ownership.
- Audit state transitions for all lifecycle changes: create, archive, approve, reject, assign, status change, verify, close.
- MinIO is used for all file/object storage; services store metadata and object keys only.
